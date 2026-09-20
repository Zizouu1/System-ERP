package com.example.backend.modules.admin.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Product Master Entity
 * Source of truth for product references, designations, and product types.
 * Used by Nomenclature, Stock, and all production operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product_master")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String ref;

    @Column(nullable = false, length = 255)
    private String designation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductTypeEnum productType;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
