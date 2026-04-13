package com.example.backend.modules.production.departement2.controller;

import com.example.backend.modules.production.departement2.dto.PfProductionRequest;
import com.example.backend.modules.production.departement2.entity.PfProduction;
import com.example.backend.modules.production.departement2.service.PfProductionService;
import com.example.backend.modules.admin.usermanagement.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/production/dept2")
@RequiredArgsConstructor
public class PfProductionController {

    private final PfProductionService pfProductionService;

    @PostMapping
    @PreAuthorize("hasRole('PF')")
    public ResponseEntity<?> createProduction(@RequestBody PfProductionRequest request,
            @AuthenticationPrincipal User user) {
        try {
            PfProduction savedProduction = pfProductionService.saveProduction(request, user);
            return ResponseEntity.ok(savedProduction);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PF')")
    public ResponseEntity<PfProduction> updateProduction(@PathVariable Long id,
            @RequestBody PfProductionRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(pfProductionService.updateProduction(id, request, user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduction(@PathVariable Long id,
            @AuthenticationPrincipal User user) {
        pfProductionService.deleteProduction(id, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PF', 'ADMIN')")
    public ResponseEntity<List<PfProduction>> getAllProductions(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(pfProductionService.getProductionHistory(user));
    }
}
