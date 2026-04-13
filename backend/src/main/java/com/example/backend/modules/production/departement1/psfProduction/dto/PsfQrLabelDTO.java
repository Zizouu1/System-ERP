package com.example.backend.modules.production.departement1.psfProduction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PsfQrLabelDTO {
    private String reference;
    private Double quantity;
    private String lotNumber;
    private String qrImageBase64;
    private LocalDateTime generatedAt;
}
