package com.example.backend.modules.production.departement2.controller;

import com.example.backend.modules.production.departement2.dto.PfProductionRequest;
import com.example.backend.modules.production.departement2.entity.PfProduction;
import com.example.backend.modules.production.departement2.service.PfProductionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/production/dept2")
@RequiredArgsConstructor
public class PfProductionController {

    private final PfProductionService pfProductionService;

    @PostMapping
    @PreAuthorize("hasRole('PF')")
    public ResponseEntity<PfProduction> createProduction(@RequestBody PfProductionRequest request) {
        try {
            PfProduction savedProduction = pfProductionService.saveProduction(request);
            return ResponseEntity.ok(savedProduction);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PfProduction> updateProduction(@PathVariable Long id,
            @RequestBody PfProductionRequest request) {
        return ResponseEntity.ok(pfProductionService.updateProduction(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduction(@PathVariable Long id) {
        pfProductionService.deleteProduction(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PF', 'ADMIN')")
    public ResponseEntity<List<PfProduction>> getAllProductions() {
        return ResponseEntity.ok(pfProductionService.getAllProductions());
    }
}
