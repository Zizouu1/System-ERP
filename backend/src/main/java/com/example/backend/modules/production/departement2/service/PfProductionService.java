package com.example.backend.modules.production.departement2.service;

import com.example.backend.modules.production.departement2.dto.PfProductionRequest;
import com.example.backend.modules.production.departement2.entity.PfProduction;
import com.example.backend.modules.production.departement2.repository.PfProductionRepository;
import com.example.backend.modules.admin.product.entity.ProductTypeEnum;
import com.example.backend.modules.production.productionstock.service.ProductService;
import com.example.backend.modules.production.productionstock.util.ProductionTimeCalculator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PfProductionService {

    private final PfProductionRepository pfProductionRepository;
    private final ProductService globalStockProductService;
    private final com.example.backend.modules.admin.product.service.ProductService masterProductService;

    @Transactional
    /**
     * Valide que la référence existe dans le Product master.
     * Vérifie que le type de produit est 'produit fini' pour la production PF.
     * Lance une exception si la référence n'existe pas ou si le type est incorrect.
     */
    private void validateProductReference(String ref) {
        masterProductService.validateProductReference(ref);
        var product = masterProductService.getProductByRef(ref);
        if (!"produit fini".equalsIgnoreCase(product.getProductType().getDisplayName())) {
            throw new IllegalArgumentException(
                "Le produit '" + ref + "' doit être de type 'produit fini'.");
        }
    }

    @Transactional
    public PfProduction saveProduction(PfProductionRequest request) {
        // Validation du produit
        validateProductReference(request.getReference());
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new IllegalArgumentException("L'heure de début et de fin sont obligatoires.");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("La quantité doit être supérieure à zéro.");
        }
        LocalTime startTimeParsed = parseTime(request.getStartTime());
        LocalTime endTimeParsed = parseTime(request.getEndTime());

        ProductionTimeCalculator.ProductionMetrics metrics = ProductionTimeCalculator.calculate(
                startTimeParsed,
                endTimeParsed,
                request.getQuantity().doubleValue());

        PfProduction production = PfProduction.builder()
                .operatorMatricule(request.getOperatorMatricule())
                .reference(request.getReference())
                .quantity(request.getQuantity())
                .scrapQuantity(request.getScrapQuantity())
                .startTime(startTimeParsed)
                .endTime(endTimeParsed)
                .rawTime(metrics.rawTime())
                .effectiveTime(metrics.effectiveTime())
                .performance(metrics.performance())
                .build();

    PfProduction saved = pfProductionRepository.save(production);
        int netQuantity = request.getQuantity() - request.getScrapQuantity();

        globalStockProductService.declareProduction(
                request.getReference(),
                netQuantity,
                "PF-" + saved.getId(),
                ProductTypeEnum.PRODUIT_FINI);

        return saved;
    }

    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty())
            return null;
        try {
            return OffsetDateTime.parse(timeStr).toLocalTime();
        } catch (DateTimeParseException e) {
            try {
                return LocalTime.parse(timeStr);
            } catch (DateTimeParseException e2) {
                try {
                    return LocalDateTime.parse(timeStr).toLocalTime();
                } catch (DateTimeParseException e3) {
                    throw new IllegalArgumentException("Invalid time format: " + timeStr);
                }
            }
        }
    }

    public List<PfProduction> getAllProductions() {
        return pfProductionRepository.findAll();
    }

    public PfProduction getProductionById(Long id) {
    return pfProductionRepository.findById(id).orElse(null);
    }

    @Transactional
    public PfProduction updateProduction(Long id, PfProductionRequest request) {
        // Validation du produit
        validateProductReference(request.getReference());
    PfProduction existing = pfProductionRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Production non trouvée"));

        int oldNetQty = existing.getQuantity() - existing.getScrapQuantity();
        globalStockProductService.revertProduction(existing.getReference(), oldNetQty, "PF-" + existing.getId());

        int newNetQty = request.getQuantity() - request.getScrapQuantity();
        if (newNetQty < 0) {
            throw new IllegalArgumentException("La quantité de rebut ne peut pas dépasser la quantité totale.");
        }

        existing.setReference(request.getReference());
        existing.setOperatorMatricule(request.getOperatorMatricule());
        existing.setQuantity(request.getQuantity());
        existing.setScrapQuantity(request.getScrapQuantity());

        if (request.getStartTime() != null && request.getEndTime() != null) {
            LocalTime start = parseTime(request.getStartTime());
            LocalTime end = parseTime(request.getEndTime());
            existing.setStartTime(start);
            existing.setEndTime(end);

            ProductionTimeCalculator.ProductionMetrics metrics = ProductionTimeCalculator.calculate(start, end,
                    request.getQuantity().doubleValue());
            existing.setRawTime(metrics.rawTime());
            existing.setEffectiveTime(metrics.effectiveTime());
            existing.setPerformance(metrics.performance());
        }

        globalStockProductService.declareProduction(request.getReference(), newNetQty, "PF-" + existing.getId(), ProductTypeEnum.PRODUIT_FINI);

        return pfProductionRepository.save(existing);
    }

    @Transactional
    public void deleteProduction(Long id) {
    PfProduction existing = pfProductionRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Production introuvable"));

        int oldNetQty = existing.getQuantity() - existing.getScrapQuantity();
        globalStockProductService.revertProduction(existing.getReference(), oldNetQty, "PF-" + existing.getId());

        pfProductionRepository.delete(existing);
    }
}