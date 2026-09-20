package com.example.backend.modules.production.departement1.shared.entity;

import com.example.backend.modules.admin.product.entity.ProductTypeEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stock_dep1", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "reference" })
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockDep1 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String reference;

    @Column(nullable = false)
    private Double totalQuantity;

    @Column(nullable = false)
    private Double storeQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductTypeEnum productType;

    @Transient
    public double getTransferedQuantity() {
        return totalQuantity != null && storeQuantity != null ? totalQuantity - storeQuantity : 0.0;
    }

    @PrePersist
    protected void onCreate() {
        if (this.totalQuantity == null)
            this.totalQuantity = 0.0;
        if (this.storeQuantity == null)
            this.storeQuantity = 0.0;
    }
}
