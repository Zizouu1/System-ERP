package com.example.backend.modules.admin.product.dto;

import com.example.backend.modules.admin.product.entity.ProductTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String ref;
    private String designation;
    private ProductTypeEnum productType;
    private String displayType; // French display name
    private Boolean active;
}
