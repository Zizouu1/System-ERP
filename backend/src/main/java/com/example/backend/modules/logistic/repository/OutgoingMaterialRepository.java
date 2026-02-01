package com.example.backend.modules.logistic.repository;

import com.example.backend.modules.logistic.entity.OutgoingMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutgoingMaterialRepository extends JpaRepository<OutgoingMaterial, Long> {
}
