package com.example.backend.modules.production.departement1.repository;

import com.example.backend.modules.production.departement1.entity.PsfStock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PsfStockRepository extends JpaRepository<PsfStock, Long> {
    Optional<PsfStock> findByReferenceAndLotNumber(String reference, String lotNumber);
}
