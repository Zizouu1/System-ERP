package com.example.backend.modules.production.departement1.productsInterne.entity;

// Removed unused import
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
@Table(name = "psf_production")
public class PsfProduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reference;

    private double quantity;

    private double quantityPerBatch;

    private int batches;

    private LocalDateTime timestamp;

    private boolean producedByCutMachine;

    private int scrapQuantity;

    @Column(nullable = false)
    private String operatorMatricule;

    private LocalTime startTime;
    private LocalTime endTime;
    private Long rawTime;
    private Long effectiveTime;
    private Double performance;
}
