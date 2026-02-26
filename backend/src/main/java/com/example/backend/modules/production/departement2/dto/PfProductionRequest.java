package com.example.backend.modules.production.departement2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PfProductionRequest {
    private String operatorMatricule;
    private String reference;
    private Integer quantity;
    private String startTime;
    private String endTime;
    @Builder.Default
    private int scrapQuantity = 0;

}
