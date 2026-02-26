package com.example.backend.modules.admin.nomenclature.repository;

import com.example.backend.modules.admin.nomenclature.entity.Nomenclature;
import com.example.backend.modules.production.productionstock.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NomenclatureRepository extends JpaRepository<Nomenclature, Long> {
    List<Nomenclature> findByParentProduct(Product parentProduct);
}
