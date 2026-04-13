package com.example.backend.modules.admin.simulation.service;

import com.example.backend.modules.admin.simulation.dto.SimulationRequest;
import com.example.backend.modules.admin.simulation.dto.SimulationResponse;
import com.example.backend.modules.admin.nomenclature.entity.Nomenclature;
import com.example.backend.modules.production.productionstock.entity.GlobalStock;
import com.example.backend.modules.admin.nomenclature.repository.NomenclatureRepository;
import com.example.backend.modules.production.productionstock.repository.GlobalStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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

    public SimulationResponse checkFeasibility(SimulationRequest request) {
        logger.info("=== SIMULATION START ===");
        logger.info("Input - ProductId: {}, Reference: {}, Quantity: {}", 
                   request.getProductId(), request.getReference(), request.getQuantity());

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new RuntimeException("Quantity must be greater than 0 for simulation.");
        }

        String reference = request.getReference();
        if (reference == null && request.getProductId() != null) {
            GlobalStock product = globalStockRepository.findById(Objects.requireNonNull(request.getProductId()))
                    .orElseThrow(() -> new RuntimeException("Product not found: " + request.getProductId()));
            reference = product.getRef();
            logger.info("Resolved productId {} to reference: {}", request.getProductId(), reference);
        }

        if (reference == null) {
            throw new RuntimeException("Reference or Product ID must be provided for simulation.");
        }

        Map<String, Double> requirements = new HashMap<>();
        try {
            calculateRequirements(reference, request.getQuantity(), requirements, new LinkedList<>(), new HashSet<>());
        } catch (RuntimeException e) {
            logger.error("Error calculating requirements: {}", e.getMessage());
            return SimulationResponse.builder()
                    .possible(false)
                    .message(e.getMessage())
                    .missingItems(new ArrayList<>())
                    .requiredItems(new ArrayList<>())
                    .build();
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
                        .build());
            }
        }

        logger.info("Overall feasible: {}", feasible);
        logger.info("Missing items: {}", missingItems);
        logger.info("=== SIMULATION END ===");

        return SimulationResponse.builder()
                .possible(feasible)
                .missingItems(missingItems)
                .requiredItems(requiredItems)
                .message(feasible ? "All components are available in stock." 
                                  : "Insufficient stock for " + missingItems.size() + " components")
                .build();
    }

    private void calculateRequirements(
            String parentRef,
            Double quantity,
            Map<String, Double> requirements,
            Deque<String> recursionPath,
            Set<String> pathSet
    ) {
        logger.info("calculateRequirements() - parentRef: {}, quantity: {}", parentRef, quantity);

        if (pathSet.contains(parentRef)) {
            String cyclePath = String.join(" -> ", recursionPath) + " -> " + parentRef;
            throw new RuntimeException("Cyclic BOM detected: " + cyclePath);
        }

        recursionPath.addLast(parentRef);
        pathSet.add(parentRef);
        
        List<Nomenclature> components = nomenclatureRepository.findByParentRef(parentRef).stream()
                .sorted(Comparator.comparing(Nomenclature::getComponentRef)
                        .thenComparing(Nomenclature::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        logger.info("Found {} BOM entries for parentRef '{}'", components.size(), parentRef);
        
        for (Nomenclature n : components) {
            logger.info("  BOM: {} -> {} (qty: {})", parentRef, n.getComponentRef(), n.getQuantityRequired());
        }

        if (components.isEmpty()) {
            Double accumulatedQty = requirements.getOrDefault(parentRef, 0.0) + quantity;
            requirements.put(parentRef, accumulatedQty);
            logger.info("Added leaf component {} with accumulated quantity: {}", parentRef, accumulatedQty);
            recursionPath.removeLast();
            pathSet.remove(parentRef);
            return;
        }

        for (Nomenclature n : components) {
            if (n.getQuantityRequired() == null || n.getQuantityRequired() <= 0) {
                throw new RuntimeException("Invalid BOM quantity for " + parentRef + " -> " + n.getComponentRef());
            }

            Double neededQty = quantity * n.getQuantityRequired();
            logger.info("Recursing into component {} with qty {} * {} = {}", 
                       n.getComponentRef(), quantity, n.getQuantityRequired(), neededQty);
            calculateRequirements(n.getComponentRef(), neededQty, requirements, recursionPath, pathSet);
        }

        recursionPath.removeLast();
        pathSet.remove(parentRef);
    }
}
