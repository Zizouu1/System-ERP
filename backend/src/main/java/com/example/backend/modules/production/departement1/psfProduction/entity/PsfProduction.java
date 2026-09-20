package com.example.backend.modules.production.departement1.psfProduction.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.example.backend.modules.production.shared.entity.Production;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "psf_production")
public class PsfProduction extends Production {

    private double quantityPerBatch;

    private int batches;

    private LocalDateTime timestamp;

    private boolean producedByCutMachine;


    @JsonIgnore
    private String stockProductionRef;
}
