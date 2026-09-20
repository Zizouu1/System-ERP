package com.example.backend.modules.production.departement1.shared.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "outgoing")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Outgoing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String reference;

    @Column(nullable = false)
    private String lotNumber;

    @Column(nullable = false)
    private Double quantityOut;

    @Column(nullable = false)
    private LocalDateTime operationDate;

    @JsonIgnore
    private String createdByUsername;

    private LocalDateTime lastModifiedAt;

    private String lastModifiedBy;

    @PreUpdate
    void onUpdate() {
        lastModifiedAt = LocalDateTime.now();
    }
}
