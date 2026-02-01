package com.example.backend.modules.logistic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutgoingMaterialDTO {
    private String reference;
    private String lotNumber;
    private Double quantityOut;
    private LocalDateTime operationDate;
    private String notes;
}
