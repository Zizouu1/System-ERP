package com.example.backend.modules.production.audit.repository;

import com.example.backend.modules.production.audit.entity.OperationAuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OperationAuditEntryRepository extends JpaRepository<OperationAuditEntry, Long> {

    List<OperationAuditEntry> findByEntityTypeAndEntityIdOrderByChangedAtDesc(String entityType, Long entityId);
}
