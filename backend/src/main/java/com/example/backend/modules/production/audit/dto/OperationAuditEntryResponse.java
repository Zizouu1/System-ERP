package com.example.backend.modules.production.audit.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Builder
public record OperationAuditEntryResponse(
        Long id,
        String entityType,
        Long entityId,
        String changedByUsername,
        LocalDateTime changedAt,
        Map<String, Object> oldValues,
        Map<String, Object> newValues) {
}
