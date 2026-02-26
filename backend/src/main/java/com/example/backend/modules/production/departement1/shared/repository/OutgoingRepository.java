package com.example.backend.modules.production.departement1.shared.repository;

import com.example.backend.modules.production.departement1.shared.entity.Outgoing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutgoingRepository extends JpaRepository<Outgoing, Long> {
}
