package com.example.backend.modules.production.productionstock.entity;

import jakarta.persistence.*;
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
@Table(name = "production_detail")
public class ProductionDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String productionRef;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "component_product_id", nullable = false)
    private GlobalStock componentProduct;

    @Column(nullable = false)
    private Double quantityConsumed;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
