package com.example.backend.modules.production.shared.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
public abstract class Production {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String operatorMatricule;

    @Column(nullable = false)
    private String reference;

    @Column(nullable = false)
    private Double quantity;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    @Builder.Default
    private int scrapQuantity = 0;

    @JsonIgnore
    private String createdByUsername;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastModifiedAt;

    private String lastModifiedBy;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        // Only record a modification timestamp when a user explicitly modified the record.
        // Internal saves (e.g. writing back stockProductionRef after creation) do NOT set
        // lastModifiedBy, so we skip updating the timestamp for those.
        if (lastModifiedBy != null) {
            lastModifiedAt = LocalDateTime.now();
        }
    }
}
