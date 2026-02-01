package com.example.backend.modules.logistic.repository;

import com.example.backend.modules.logistic.entity.IncomingMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncomingMaterialRepository extends JpaRepository<IncomingMaterial, Long> {
}
