package com.example.backend.modules.admin.productiondelay.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductionDelayPredictionRequest {

    private Double duree;
    private Double quantiteCommandee;
    private Double machinesDisponibles;
    private Double bomDepth;
    private Double totalOperations;
    private Double totalBomComponents;
}
