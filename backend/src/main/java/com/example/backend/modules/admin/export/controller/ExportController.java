package com.example.backend.modules.admin.export.controller;

import com.example.backend.modules.admin.export.dto.ExportRequest;
import com.example.backend.modules.admin.export.entity.Export;
import com.example.backend.modules.admin.export.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exports")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @GetMapping
    public ResponseEntity<List<Export>> getAllExports() {
        return ResponseEntity.ok(exportService.getAllExports());
    }

    @PostMapping
    public ResponseEntity<Export> createExport(@RequestBody ExportRequest request) {
        return ResponseEntity.ok(exportService.createExport(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Export> updateExport(@PathVariable Long id, @RequestBody ExportRequest request) {
        return ResponseEntity.ok(exportService.updateExport(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExport(@PathVariable Long id) {
        exportService.deleteExport(id);
        return ResponseEntity.noContent().build();
    }
}
