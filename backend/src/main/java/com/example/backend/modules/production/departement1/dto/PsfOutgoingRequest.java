package com.example.backend.modules.production.departement1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PsfOutgoingRequest {
    private String reference;
    private String lotNumber;
    private double quantityOut;
    private String notes;
    private Boolean isInternal;
    private LocalDateTime operationDate;
}
