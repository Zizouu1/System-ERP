package com.example.backend.modules.production.shared.repository;

import com.example.backend.modules.production.shared.entity.Nomenclature;
import com.example.backend.modules.production.shared.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NomenclatureRepository extends JpaRepository<Nomenclature, Long> {
    List<Nomenclature> findByParentProduct(Product parentProduct);
}
