package com.example.backend.modules.production.shared.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Map;

/**
 * Utility class for creating snapshots of entities for audit logging.
 */
public final class SnapshotUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private SnapshotUtil() {
        // Prevent instantiation
    }

    /**
     * Converts an entity to a Map suitable for audit logging.
     * @param entity the entity to snapshot
     * @return a Map containing the entity's fields
     */
    public static Map<String, Object> snapshot(Object entity) {
        if (entity == null) {
            return Map.of();
        }
        return objectMapper.convertValue(entity, new TypeReference<Map<String, Object>>() {});
    }
}
