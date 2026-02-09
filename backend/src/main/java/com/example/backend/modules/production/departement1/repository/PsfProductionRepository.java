package com.example.backend.modules.production.departement1.repository;

import com.example.backend.modules.production.departement1.entity.PsfProduction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PsfProductionRepository extends JpaRepository<PsfProduction, Long> {
}
