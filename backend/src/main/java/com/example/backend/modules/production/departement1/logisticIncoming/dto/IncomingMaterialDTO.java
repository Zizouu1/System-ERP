package com.example.backend.modules.production.departement1.logisticIncoming.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomingMaterialDTO {
    private String reference;
    private Double quantity;
    private String lotNumber;
    private String operatorMatricule;
    private String notes;
    private LocalDateTime operationDate;
    private Boolean generateQr;
}
