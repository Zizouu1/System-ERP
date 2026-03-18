package com.example.backend.modules.admin.export.entity;

import com.example.backend.modules.production.productionstock.entity.GlobalStock;
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
@Table(name = "export")
public class Export {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private GlobalStock product;

    @Column(nullable = false)
    private Double quantity;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime exportDate = LocalDateTime.now();

}
