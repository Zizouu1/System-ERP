package com.example.backend.modules.production.departement1.psfProduction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PsfProductionRequest {
    private String reference;
    private double quantity;
    private double quantityPerBatch;
    private String operatorMatricule;
    private int scrapQuantity;
    private boolean producedByCutMachine;
    private String startTime;
    private String endTime;
}
