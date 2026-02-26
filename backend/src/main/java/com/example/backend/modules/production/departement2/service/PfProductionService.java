package com.example.backend.modules.production.departement2.service;

import com.example.backend.modules.production.departement2.dto.PfProductionRequest;
import com.example.backend.modules.production.departement2.entity.PfProduction;
import com.example.backend.modules.production.departement2.repository.PfProductionRepository;
import com.example.backend.modules.production.productionstock.service.ProductService;
import com.example.backend.modules.production.productionstock.util.ProductionTimeCalculator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PfProductionService {

    private final PfProductionRepository pfProductionRepository;
    private final ProductService productService;

    @Transactional
    public PfProduction saveProduction(PfProductionRequest request) {
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new IllegalArgumentException("Start time and end time are required.");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        // 1. Calculate time metrics
        LocalTime startTimeParsed = LocalTime.parse(request.getStartTime());
        LocalTime endTimeParsed = LocalTime.parse(request.getEndTime());

        ProductionTimeCalculator.ProductionMetrics metrics = ProductionTimeCalculator.calculate(
                startTimeParsed,
                endTimeParsed,
                request.getQuantity().doubleValue());

        // 2. Save PfProduction record
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

        // 3. Call ProductService: read BOM, consume components, increase produced
        // product, save ProductionDetails
        productService.declareProduction(
                request.getReference(),
                netQuantity,
                "PF-" + saved.getId());

        return saved;
    }

    public List<PfProduction> getAllProductions() {
        return pfProductionRepository.findAll();
    }

    public PfProduction getProductionById(Long id) {
        return pfProductionRepository.findById(id).orElse(null);
    }

    @Transactional
    public PfProduction updateProduction(Long id, PfProductionRequest request) {
        PfProduction existing = pfProductionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Production not found"));

        int oldNetQty = existing.getQuantity() - existing.getScrapQuantity();
        productService.revertProduction(existing.getReference(), oldNetQty, "PF-" + existing.getId());

        int newNetQty = request.getQuantity() - request.getScrapQuantity();
        if (newNetQty < 0) {
            throw new IllegalArgumentException("Scrap cannot exceed total quantity");
        }

        existing.setReference(request.getReference());
        existing.setOperatorMatricule(request.getOperatorMatricule());
        existing.setQuantity(request.getQuantity());
        existing.setScrapQuantity(request.getScrapQuantity());

        if (request.getStartTime() != null && request.getEndTime() != null) {
            LocalTime start = LocalTime.parse(request.getStartTime());
            LocalTime end = LocalTime.parse(request.getEndTime());
            existing.setStartTime(start);
            existing.setEndTime(end);

            ProductionTimeCalculator.ProductionMetrics metrics = ProductionTimeCalculator.calculate(start, end,
                    request.getQuantity().doubleValue());
            existing.setRawTime(metrics.rawTime());
            existing.setEffectiveTime(metrics.effectiveTime());
            existing.setPerformance(metrics.performance());
        }

        productService.declareProduction(request.getReference(), newNetQty, "PF-" + existing.getId());

        return pfProductionRepository.save(existing);
    }

    @Transactional
    public void deleteProduction(Long id) {
        PfProduction existing = pfProductionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Production not found"));

        int oldNetQty = existing.getQuantity() - existing.getScrapQuantity();
        productService.revertProduction(existing.getReference(), oldNetQty, "PF-" + existing.getId());

        pfProductionRepository.delete(existing);
    }
}