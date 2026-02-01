package com.example.backend.modules.logistic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStockDTO {
    private String reference;
    private Double totalQuantity;
    private Double storeQuantity;
    private Double consumedQuantity;
}
