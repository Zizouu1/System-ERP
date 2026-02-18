package com.example.backend.modules.production.departement1.entity;

import com.example.backend.modules.admin.usermanagement.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder.Default
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "psf_production")
public class PsfProduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reference;

    private double totalProducedQuantity;

    private double quantityPerBatch;

    private int batches;

    private String productionId;

    private LocalDateTime timestamp;

    private boolean producedByCutMachine;

    private int scrapQuantity = 0;

    @Column(nullable = false)
    private String operatorMatricule;

    private LocalTime startTime;
    private LocalTime endTime;
    private Long rawTime;
    private Long effectiveTime;
    private Double performance;
}
