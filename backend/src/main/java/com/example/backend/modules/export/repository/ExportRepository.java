package com.example.backend.modules.export.repository;

import com.example.backend.modules.export.entity.Export;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExportRepository extends JpaRepository<Export, Long> {
    List<Export> findAllByOrderByExportDateDesc();
}
