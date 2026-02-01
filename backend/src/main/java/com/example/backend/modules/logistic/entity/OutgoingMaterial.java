package com.example.backend.modules.logistic.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "outgoing_materials")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutgoingMaterial {

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

    @Column(length = 500)
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (this.operationDate == null) {
            this.operationDate = LocalDateTime.now();
        }
    }
}
