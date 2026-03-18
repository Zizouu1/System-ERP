package com.example.backend.modules.production.departement1.shared.repository;

import com.example.backend.modules.production.departement1.shared.entity.StockDep1;
import com.example.backend.modules.production.departement1.shared.entity.StockSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockDep1Repository extends JpaRepository<StockDep1, Long> {
    Optional<StockDep1> findByReferenceAndLotNumberAndSource(String reference, String lotNumber, StockSource source);

    Optional<StockDep1> findByReferenceAndSource(String reference, StockSource source);

    List<StockDep1> findByReferenceAndSourceAndLotNumberStartingWith(String reference, StockSource source, String lotPrefix);

    List<StockDep1> findBySource(StockSource source);
}
