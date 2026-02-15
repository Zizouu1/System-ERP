package com.example.backend.modules.production.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionDeclarationRequest {
    private String productRef;
    private Double quantity;
}
