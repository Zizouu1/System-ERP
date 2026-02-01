package com.example.backend.modules.logistic.repository;

import com.example.backend.modules.logistic.entity.ProductStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ProductStockRepository extends JpaRepository<ProductStock, Long> {
    Optional<ProductStock> findByReference(String reference);
}
