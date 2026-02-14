package com.example.backend.modules.production.departement2.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String productReference;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    // Calculated fields
    private Double performance;
    private Long effectiveTime; // In minutes
    private Long rawTime; // In minutes

    // PrePersist/PreUpdate or Service logic will handle calculations
}
