package com.example.backend.modules.production.departement1.entity;

import com.example.backend.modules.production.shared.entity.ProductType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "psf_stock", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "reference", "lotNumber" })
})
public class PsfStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reference;

    private String lotNumber;

    @Enumerated(EnumType.STRING)
    private ProductType productType;

    private double totalQuantity;

    private double storeQuantity;

    private boolean isInternal;

    @Transient
    public double getConsumedQuantity() {
        return totalQuantity - storeQuantity;
    }
}
