package com.example.backend.modules.production.productionstock.repository;

import com.example.backend.modules.production.productionstock.entity.GlobalStock;
import com.example.backend.modules.admin.product.entity.ProductTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GlobalStockRepository extends JpaRepository<GlobalStock, Long> {
    Optional<GlobalStock> findByRef(String ref);

    List<GlobalStock> findByType(ProductTypeEnum type);
}
