package com.example.backend.modules.admin.productiondelay.controller;

import com.example.backend.modules.admin.productiondelay.dto.ProductionDelayPredictionRequest;
import com.example.backend.modules.admin.productiondelay.dto.ProductionDelayPredictionResponse;
import com.example.backend.modules.admin.productiondelay.service.ProductionDelayPredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/production-delay")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ProductionDelayPredictionController {

    private final ProductionDelayPredictionService productionDelayPredictionService;

    @PostMapping("/predict")
    public ResponseEntity<ProductionDelayPredictionResponse> predictDelay(
            @RequestBody ProductionDelayPredictionRequest request
    ) {
        return ResponseEntity.ok(productionDelayPredictionService.predictDelay(request));
    }

}

