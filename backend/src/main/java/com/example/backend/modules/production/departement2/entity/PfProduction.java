package com.example.backend.modules.production.departement2.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pf_production")
public class PfProduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String operatorMatricule;

    @Column(nullable = false)
    private String reference;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Builder.Default
    private int scrapQuantity = 0;

    // Calculated fields
    private Double performance;
    private Long effectiveTime; // In minutes
    private Long rawTime; // In minutes

    @JsonIgnore
    private String createdByUsername;

    @Builder.Default
    private Boolean modified = false;

    private LocalDateTime lastModifiedAt;

    private String lastModifiedBy;
}
