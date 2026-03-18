package com.example.backend.modules.admin.nomenclature.entity;

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
@Table(name = "nomenclature")
public class Nomenclature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_ref", nullable = false)
    private String parentRef;

    @Column(name = "component_ref", nullable = false)
    private String componentRef;

    @Column(nullable = false)
    private Double quantityRequired;
}
