package com.example.backend.modules.production.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "operation_audit_entry")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationAuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String entityType;

    @Column(nullable = false)
    private Long entityId;

    @Column(nullable = false)
    private String changedByUsername;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime changedAt = LocalDateTime.now();

    @Lob
    @Column(nullable = false)
    private String oldValuesJson;

    @Lob
    private String newValuesJson;
}
