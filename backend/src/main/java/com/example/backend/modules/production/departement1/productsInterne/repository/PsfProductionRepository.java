package com.example.backend.modules.production.departement1.productsInterne.repository;

import com.example.backend.modules.production.departement1.productsInterne.entity.PsfProduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PsfProductionRepository extends JpaRepository<PsfProduction, Long> {
}
