package com.example.backend.modules.export.controller;

import com.example.backend.modules.export.dto.ExportRequest;
import com.example.backend.modules.export.entity.Export;
import com.example.backend.modules.export.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exports")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LOGISTIC')")
    public ResponseEntity<List<Export>> getAllExports() {
        return ResponseEntity.ok(exportService.getAllExports());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LOGISTIC')")
    public ResponseEntity<Export> createExport(@RequestBody ExportRequest request) {
        return ResponseEntity.ok(exportService.createExport(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LOGISTIC')")
    public ResponseEntity<Export> updateExport(@PathVariable Long id, @RequestBody ExportRequest request) {
        return ResponseEntity.ok(exportService.updateExport(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LOGISTIC')")
    public ResponseEntity<Void> deleteExport(@PathVariable Long id) {
        exportService.deleteExport(id);
        return ResponseEntity.noContent().build();
    }
}
