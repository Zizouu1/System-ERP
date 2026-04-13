package com.example.backend.modules.production.audit.service;

import com.example.backend.modules.admin.notification.service.AdminNotificationService;
import com.example.backend.modules.admin.usermanagement.entity.User;
import com.example.backend.modules.production.audit.dto.OperationAuditEntryResponse;
import com.example.backend.modules.production.audit.entity.OperationAuditEntry;
import com.example.backend.modules.production.audit.repository.OperationAuditEntryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OperationAuditService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final OperationAuditEntryRepository repository;
    private final ObjectMapper objectMapper;
    private final AdminNotificationService adminNotificationService;

    @Transactional
    public void recordUpdate(String entityType, Long entityId, Object oldValues, Object newValues, User actor) {
        save(entityType, entityId, "UPDATE", oldValues, newValues, actor);
    }

    @Transactional
    public void recordDelete(String entityType, Long entityId, Object oldValues, User actor) {
        save(entityType, entityId, "DELETE", oldValues, null, actor);
    }

    @Transactional(readOnly = true)
    public List<OperationAuditEntryResponse> getOperationHistory(String entityType, Long entityId) {
        String normalizedEntityType = entityType == null ? "" : entityType.trim().toUpperCase();
        return repository.findByEntityTypeAndEntityIdOrderByChangedAtDesc(normalizedEntityType, entityId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void save(String entityType,
                      Long entityId,
                      String action,
                      Object oldValues,
                      Object newValues,
                      User actor) {
        OperationAuditEntry entry = OperationAuditEntry.builder()
                .entityType(entityType.toUpperCase())
                .entityId(entityId)
        .changedByUsername(actor != null ? actor.getUsername() : "system")
                .oldValuesJson(toJson(oldValues))
                .newValuesJson(toJson(newValues))
                .build();
        repository.save(entry);

    adminNotificationService.notifyOperationChange(
        entityType,
        entityId,
        action,
        actor != null ? actor.getUsername() : null);
    }

    private OperationAuditEntryResponse toResponse(OperationAuditEntry entry) {
        return OperationAuditEntryResponse.builder()
                .id(entry.getId())
                .entityType(entry.getEntityType())
                .entityId(entry.getEntityId())
                .changedByUsername(entry.getChangedByUsername())
                .changedAt(entry.getChangedAt())
                .oldValues(toMap(entry.getOldValuesJson()))
                .newValues(toMap(entry.getNewValuesJson()))
                .build();
    }

    private String toJson(Object payload) {
        try {
            if (payload == null) {
                return "{}";
            }
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de sérialiser les données d'audit.", e);
        }
    }

    private Map<String, Object> toMap(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("raw", json);
            return fallback;
        }
    }
}
