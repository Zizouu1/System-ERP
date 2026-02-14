package com.example.backend.modules.production.departement2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PfProductionRequest {
    private String operatorMatricule;
    private String productReference;
    private Integer quantity;
    private LocalTime startTime;
    private LocalTime endTime;
}
