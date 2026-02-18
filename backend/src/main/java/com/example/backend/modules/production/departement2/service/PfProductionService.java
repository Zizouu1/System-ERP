package com.example.backend.modules.production.departement2.service;

import com.example.backend.modules.production.departement2.dto.PfProductionRequest;
import com.example.backend.modules.production.departement2.entity.PfProduction;
import com.example.backend.modules.production.departement2.repository.PfProductionRepository;
import com.example.backend.modules.production.shared.service.ProductService;
import com.example.backend.modules.production.shared.util.ProductionTimeCalculator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
        ProductionTimeCalculator.ProductionMetrics metrics = ProductionTimeCalculator.calculate(
                request.getStartTime(),
                request.getEndTime(),
                request.getQuantity());

        // 2. Save PfProduction record
        PfProduction production = PfProduction.builder()
                .operatorMatricule(request.getOperatorMatricule())
                .productReference(request.getProductReference())
                .quantity(request.getQuantity())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .rawTime(metrics.rawTime())
                .effectiveTime(metrics.effectiveTime())
                .performance(metrics.performance())
                .build();

        PfProduction saved = pfProductionRepository.save(production);
        int netQuantity = request.getQuantity() - request.getScrapQuantity();

        // 3. Call ProductService: read BOM, consume components, increase produced
        // product, save ProductionDetails
        productService.declareProduction(
                request.getProductReference(),
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
}