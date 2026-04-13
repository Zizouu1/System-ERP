package com.example.backend.modules.production.departement1.shared.repository;

import com.example.backend.modules.production.departement1.shared.entity.StockDep1;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockDep1Repository extends JpaRepository<StockDep1, Long> {
    Optional<StockDep1> findFirstByReferenceOrderByIdAsc(String reference);

    List<StockDep1> findByReference(String reference);
}
