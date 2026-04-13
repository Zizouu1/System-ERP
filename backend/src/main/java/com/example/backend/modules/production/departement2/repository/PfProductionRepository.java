package com.example.backend.modules.production.departement2.repository;

import com.example.backend.modules.production.departement2.entity.PfProduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PfProductionRepository extends JpaRepository<PfProduction, Long> {

    List<PfProduction> findAllByOrderByIdDesc();

    List<PfProduction> findByCreatedByUsernameOrderByIdDesc(String createdByUsername);

    List<PfProduction> findByOperatorMatriculeOrderByIdDesc(String operatorMatricule);
}
