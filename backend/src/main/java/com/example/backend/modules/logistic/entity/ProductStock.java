package com.example.backend.modules.logistic.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_stocks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    private Double totalQuantity;

    @Column(nullable = false)
    private Double storeQuantity;

    @Column(nullable = false)
    private Double consumedQuantity;

    @PrePersist
    protected void onCreate() {
        if (this.totalQuantity == null)
            this.totalQuantity = 0.0;
        if (this.storeQuantity == null)
            this.storeQuantity = 0.0;
        if (this.consumedQuantity == null)
            this.consumedQuantity = 0.0;
    }
}
