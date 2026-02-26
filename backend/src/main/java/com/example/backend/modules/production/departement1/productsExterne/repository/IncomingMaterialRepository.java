package com.example.backend.modules.production.departement1.productsExterne.repository;

import com.example.backend.modules.production.departement1.productsExterne.entity.IncomingMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncomingMaterialRepository extends JpaRepository<IncomingMaterial, Long> {
}
