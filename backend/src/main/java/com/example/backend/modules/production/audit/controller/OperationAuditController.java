package com.example.backend.modules.production.audit.controller;

import com.example.backend.modules.production.audit.dto.OperationAuditEntryResponse;
import com.example.backend.modules.production.audit.service.OperationAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit/operations")
@RequiredArgsConstructor
@Slf4j
public class OperationAuditController {

    private final OperationAuditService operationAuditService;

    @GetMapping("/{entityType}/{entityId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OperationAuditEntryResponse>> getOperationHistory(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        log.info("Operation audit history request received: entityType='{}', entityId={}", entityType, entityId);
        return ResponseEntity.ok(operationAuditService.getOperationHistory(entityType, entityId));
    }
}
