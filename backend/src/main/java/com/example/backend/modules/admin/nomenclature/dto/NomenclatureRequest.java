package com.example.backend.modules.admin.nomenclature.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NomenclatureRequest {
    private String parentRef;
    private String componentRef;
    private Double quantityRequired;
}
