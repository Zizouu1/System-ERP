package com.example.backend.modules.admin.productiondelay.controller;

import com.example.backend.modules.admin.productiondelay.dto.ModelMetricsResponse;
import com.example.backend.modules.admin.productiondelay.service.ProductionDelayPredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/model-metrics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ModelMetricsController {

    private final ProductionDelayPredictionService productionDelayPredictionService;

    @GetMapping
    public ResponseEntity<ModelMetricsResponse> getModelMetrics() {
        return ResponseEntity.ok(productionDelayPredictionService.fetchModelMetrics());
    }
}