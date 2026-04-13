package com.example.backend.modules.production.departement1.psfProduction.repository;

import com.example.backend.modules.production.departement1.psfProduction.entity.PsfProduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PsfProductionRepository extends JpaRepository<PsfProduction, Long> {

    List<PsfProduction> findAllByOrderByIdDesc();

    List<PsfProduction> findByCreatedByUsernameOrderByIdDesc(String createdByUsername);

    List<PsfProduction> findByOperatorMatriculeOrderByIdDesc(String operatorMatricule);
}
