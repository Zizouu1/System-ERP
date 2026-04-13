package com.example.backend.modules.production.productionstock.repository;

import com.example.backend.modules.production.productionstock.entity.ProductionDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductionDetailRepository extends JpaRepository<ProductionDetail, Long> {
    List<ProductionDetail> findByProductionRef(String productionRef);

    boolean existsByComponentProductId(Long componentProductId);
}
