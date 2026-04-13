package com.example.backend.modules.production.departement1.shared.repository;

import com.example.backend.modules.production.departement1.shared.entity.Outgoing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutgoingRepository extends JpaRepository<Outgoing, Long> {

    List<Outgoing> findAllByOrderByIdDesc();

    List<Outgoing> findByCreatedByUsernameOrderByIdDesc(String createdByUsername);
}
