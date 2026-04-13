package com.example.backend.modules.admin.product.repository;

import com.example.backend.modules.admin.product.entity.Product;
import com.example.backend.modules.admin.product.entity.ProductTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByRef(String ref);
    
    boolean existsByRef(String ref);

    List<Product> findByProductType(ProductTypeEnum productType);

    List<Product> findByActive(Boolean active);

    List<Product> findByRefContainingIgnoreCaseOrDesignationContainingIgnoreCase(String ref, String designation);
}
