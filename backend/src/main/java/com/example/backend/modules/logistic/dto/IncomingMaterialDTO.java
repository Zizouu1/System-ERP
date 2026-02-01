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
public class IncomingMaterialDTO {
    private String reference;
    private Double quantity;
    private String lotNumber;
    private LocalDateTime operationDate;
    private String notes;
}
