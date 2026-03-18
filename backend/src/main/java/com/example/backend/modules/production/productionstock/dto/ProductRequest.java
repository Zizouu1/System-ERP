package com.example.backend.modules.production.productionstock.dto;

import com.example.backend.modules.admin.product.entity.ProductTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {
    private String ref;
    private ProductTypeEnum type;
}
