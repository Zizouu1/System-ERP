package com.example.backend.modules.production.departement2.service;

import com.example.backend.modules.production.departement2.dto.PfProductionRequest;
import com.example.backend.modules.production.departement2.entity.PfProduction;
import com.example.backend.modules.production.departement2.repository.PfProductionRepository;
import com.example.backend.modules.production.shared.util.ProductionTimeCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PfProductionService {

    private final PfProductionRepository pfProductionRepository;

    public PfProduction saveProduction(PfProductionRequest request) {
        ProductionTimeCalculator.ProductionMetrics metrics = ProductionTimeCalculator.calculate(
                request.getStartTime(),
                request.getEndTime(),
                request.getQuantity());

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

        return pfProductionRepository.save(production);
    }

    public List<PfProduction> getAllProductions() {
        return pfProductionRepository.findAll();
    }

    public PfProduction getProductionById(Long id) {
        return pfProductionRepository.findById(id).orElse(null);
    }
}
