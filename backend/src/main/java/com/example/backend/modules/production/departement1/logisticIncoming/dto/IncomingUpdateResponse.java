package com.example.backend.modules.production.departement1.logisticIncoming.dto;

import com.example.backend.modules.production.departement1.logisticIncoming.entity.IncomingMaterial;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record IncomingUpdateResponse(
        IncomingMaterial incoming,
        String qrImageBase64,
        String reference,
        Double quantity,
        String lotNumber,
        LocalDateTime generatedAt) {
}
