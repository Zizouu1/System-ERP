package com.example.backend.modules.production.productionstock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionDeclarationRequest {
    private String reference;
    private Double quantity;
}
