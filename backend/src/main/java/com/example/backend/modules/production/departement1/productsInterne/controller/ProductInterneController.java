package com.example.backend.modules.production.departement1.productsInterne.controller;

import com.example.backend.modules.production.departement1.productsInterne.dto.PsfProductionRequest;
import com.example.backend.modules.production.departement1.shared.entity.StockDep1;
import com.example.backend.modules.production.departement1.productsInterne.service.ProductInterneService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/psf/interne")
@RequiredArgsConstructor
public class ProductInterneController {

    private final ProductInterneService psfService;

    @PostMapping(value = "/production", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('PSF')")
    public ResponseEntity<List<byte[]>> declareProduction(@RequestBody PsfProductionRequest request) {
        List<byte[]> qrCodes = psfService.declareProduction(request);
        return ResponseEntity.ok(qrCodes);
    }

    @GetMapping("/production")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllProductions() {
        return ResponseEntity.ok(psfService.getAllProductions());
    }

    @PutMapping("/production/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProduction(@PathVariable Long id, @RequestBody PsfProductionRequest request) {
        return ResponseEntity.ok(psfService.updateProduction(id, request));
    }

    @DeleteMapping("/production/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduction(@PathVariable Long id) {
        psfService.deleteProduction(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stocks")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllStocks() {
        return ResponseEntity.ok(psfService.getAllStocks());
    }

    @PutMapping("/stocks/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateStock(@PathVariable Long id,
            @RequestBody StockDep1 request) {
        return ResponseEntity.ok(psfService.updateStock(id, request));
    }

    @DeleteMapping("/stocks/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStock(@PathVariable Long id) {
        psfService.deleteStock(id);
        return ResponseEntity.noContent().build();
    }
}
