package com.example.backend.modules.admin.productiondelay.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductionDelayPredictionRequest {

    private Integer duree;
    private Integer quantiteCommandee;
    private Integer machinesDisponibles;
    private Integer bomDepth;
    private Integer totalOperations;
    private Integer totalBomComponents;
}
