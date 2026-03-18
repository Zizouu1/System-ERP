package com.example.backend.modules.admin.productiondelay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionDelayPredictionResponse {

    private Double delayProbability;
    private String message;
    private String error;
}
