package com.example.backend.modules.production.departement1.logisticIncoming.repository;

import com.example.backend.modules.production.departement1.logisticIncoming.entity.IncomingMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncomingMaterialRepository extends JpaRepository<IncomingMaterial, Long> {

    List<IncomingMaterial> findAllByOrderByIdDesc();

    List<IncomingMaterial> findByCreatedByUsernameOrderByIdDesc(String createdByUsername);

    List<IncomingMaterial> findByOperatorMatriculeOrderByIdDesc(String operatorMatricule);
}
