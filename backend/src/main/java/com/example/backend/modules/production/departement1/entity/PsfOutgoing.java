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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "psf_outgoing")
public class PsfOutgoing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reference;

    private String lotNumber;

    private double quantityOut;

    private boolean isInternal;

    private String notes;

    private LocalDateTime operationDate;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
