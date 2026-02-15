package com.example.backend.modules.production.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String ref;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductType type;

    @Column(nullable = false)
    @Builder.Default
    private Double quantityTotal = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double quantityUsed = 0.0;

    public Double getQuantityAvailable() {
        return quantityTotal - quantityUsed;
    }
}
