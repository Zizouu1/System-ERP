package com.example.backend.modules.production.shared.repository;

import com.example.backend.modules.production.shared.entity.Product;
import com.example.backend.modules.production.shared.entity.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByRef(String ref);

    List<Product> findByType(ProductType type);
}
