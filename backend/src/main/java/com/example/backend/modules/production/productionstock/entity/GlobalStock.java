package com.example.backend.modules.production.productionstock.entity;

import jakarta.persistence.*;
import com.example.backend.modules.admin.product.entity.ProductTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Global Stock Entity
 * Tracks quantities and stock levels for products across the system.
 * (Previously called Product - renamed to clarify its purpose as inventory tracking)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "global_stock")
public class GlobalStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String ref;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductTypeEnum type;

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
