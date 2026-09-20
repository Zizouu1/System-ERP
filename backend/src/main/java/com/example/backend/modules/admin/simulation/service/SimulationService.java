package com.example.backend.modules.admin.simulation.service;

import com.example.backend.modules.admin.simulation.dto.SimulationRequest;
import com.example.backend.modules.admin.simulation.dto.SimulationResponse;
import com.example.backend.modules.admin.nomenclature.entity.Nomenclature;
import com.example.backend.modules.admin.nomenclature.repository.NomenclatureRepository;
import com.example.backend.modules.admin.product.entity.Product;
import com.example.backend.modules.admin.product.entity.ProductTypeEnum;
import com.example.backend.modules.admin.product.repository.ProductRepository;
import com.example.backend.modules.production.productionstock.entity.GlobalStock;
import com.example.backend.modules.production.productionstock.repository.GlobalStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private static final Logger logger = LoggerFactory.getLogger(SimulationService.class);

    private final GlobalStockRepository globalStockRepository;
    private final NomenclatureRepository nomenclatureRepository;
    private final ProductRepository productRepository;

    private static final Set<ProductTypeEnum> ALLOWED_TYPES = Set.of(
        ProductTypeEnum.SEMI_FINI,
        ProductTypeEnum.PRODUIT_FINI
    );

    public SimulationResponse checkFeasibility(SimulationRequest request) {
        return checkFeasibilityBatch(List.of(request));
    }

    public SimulationResponse checkFeasibilityBatch(List<SimulationRequest> requests) {
        logger.info("=== SIMULATION START ===");
        logger.info("Input batch size: {}", requests == null ? 0 : requests.size());

        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Aucun produit sélectionné pour la simulation.");
        }

    Map<String, Double> requirements = new HashMap<>();
    Map<String, List<SimulationResponse.ProductContribution>> contributions = new HashMap<>();
        for (SimulationRequest request : requests) {
            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                throw new IllegalArgumentException("La quantité doit être supérieure à zéro pour la simulation.");
            }

            Product product = resolveProduct(request);
            if (!ALLOWED_TYPES.contains(product.getProductType())) {
                throw new IllegalArgumentException("Ce type de produit ne peut pas être simulé");
            }

            String reference = product.getRef();
            logger.info("Resolved product for simulation: {} ({})", reference, product.getProductType());

            List<Nomenclature> components = nomenclatureRepository.findByParentRef(reference).stream()
                    .sorted(Comparator.comparing(Nomenclature::getComponentRef)
                            .thenComparing(Nomenclature::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();

            if (components.isEmpty()) {
                throw new IllegalArgumentException("Ce produit n'a pas de nomenclature");
            }

            for (Nomenclature component : components) {
                if (component.getQuantityRequired() == null || component.getQuantityRequired() <= 0) {
                    throw new IllegalArgumentException(
                            "Quantité de nomenclature invalide pour " + reference + " -> " + component.getComponentRef());
                }

                double neededQty = request.getQuantity() * component.getQuantityRequired();
                requirements.merge(component.getComponentRef(), neededQty, Double::sum);
                contributions
                        .computeIfAbsent(component.getComponentRef(), key -> new ArrayList<>())
                        .add(SimulationResponse.ProductContribution.builder()
                                .productRef(product.getRef())
                                .productDesignation(product.getDesignation())
                .producedQty(request.getQuantity())
                                .requiredQty(neededQty)
                                .build());
            }
        }

        Map<String, Double> orderedRequirements = new LinkedHashMap<>();
        requirements.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> orderedRequirements.put(entry.getKey(), entry.getValue()));

        logger.info("Calculated requirements: {}", orderedRequirements);

        List<SimulationResponse.MissingItem> missingItems = new ArrayList<>();
        List<SimulationResponse.RequiredItem> requiredItems = new ArrayList<>();
        boolean feasible = true;

        for (Map.Entry<String, Double> entry : orderedRequirements.entrySet()) {
            String ref = entry.getKey();
            Double required = entry.getValue();

            GlobalStock component = globalStockRepository.findByRef(ref).orElse(null);
            double available = (component != null) ? component.getQuantityAvailable() : 0.0;
            double missing = Math.max(0.0, required - available);

            requiredItems.add(SimulationResponse.RequiredItem.builder()
                    .reference(ref)
                    .requiredQty(required)
                    .availableQty(available)
                    .missingQty(missing)
                    .build());

            logger.info("Component: {} | Required: {} | Available: {} | Feasible: {}", 
                       ref, required, available, available >= required);

            if (missing > 0) {
                feasible = false;
        missingItems.add(SimulationResponse.MissingItem.builder()
            .reference(ref)
            .requiredQty(required)
            .availableQty(available)
            .missingQty(missing)
            .productContributions(contributions.getOrDefault(ref, List.of()))
            .build());
            }
        }

        logger.info("Overall feasible: {}", feasible);
        logger.info("Missing items: {}", missingItems);
        logger.info("=== SIMULATION END ===");

    String shortageMessage = feasible
        ? "All components are available in stock."
        : buildShortageMessage(missingItems);

        return SimulationResponse.builder()
                .possible(feasible)
                .missingItems(missingItems)
                .requiredItems(requiredItems)
                .message(shortageMessage)
                .build();
    }

    private Product resolveProduct(SimulationRequest request) {
        if (request.getProductId() != null) {
            return productRepository.findById(Objects.requireNonNull(request.getProductId()))
                    .orElseThrow(() -> new IllegalArgumentException("Produit introuvable"));
        }
        if (request.getReference() != null && !request.getReference().trim().isEmpty()) {
            return productRepository.findByRef(request.getReference().trim())
                    .orElseThrow(() -> new IllegalArgumentException("Produit introuvable"));
        }
        throw new IllegalArgumentException("Référence ou ID produit obligatoire pour la simulation.");
    }

    private String buildShortageMessage(List<SimulationResponse.MissingItem> missingItems) {
        if (missingItems == null || missingItems.isEmpty()) {
            return "All components are available in stock.";
        }
        return missingItems.stream()
                .map(item -> String.format(
                        "Production impossible : le composant %s requiert %.2f, stock disponible %.2f. %s",
                        item.getReference(),
                        item.getRequiredQty() != null ? item.getRequiredQty() : 0.0,
                        item.getAvailableQty() != null ? item.getAvailableQty() : 0.0,
                        formatProductContributionMessage(item.getProductContributions())))
                .reduce((first, second) -> first + " | " + second)
                .orElse("Stock insuffisant pour certains composants");
    }

    private String formatProductContributionMessage(List<SimulationResponse.ProductContribution> contributions) {
        if (contributions == null || contributions.isEmpty()) {
            return "Produits concernés : -";
        }
    String list = contributions.stream()
        .map(contribution -> String.format(
            "%s (x%s) nécessite %.2f",
            contribution.getProductRef(),
            contribution.getProducedQty() != null ? trimTrailingZeros(contribution.getProducedQty()) : "-",
            contribution.getRequiredQty() != null ? contribution.getRequiredQty() : 0.0))
                .reduce((first, second) -> first + ", " + second)
                .orElse("-");
        return "Produits concernés : " + list;
    }

    private String trimTrailingZeros(Double value) {
    if (value == null) return "-";
    if (value % 1 == 0) return String.valueOf(value.intValue());
    return String.format("%.2f", value);
    }
}
