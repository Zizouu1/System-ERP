package com.example.backend.modules.production.departement1.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutgoingDTO {
    private String reference;
    private String lotNumber;
    private Double quantityOut;
    private LocalDateTime operationDate;
}
