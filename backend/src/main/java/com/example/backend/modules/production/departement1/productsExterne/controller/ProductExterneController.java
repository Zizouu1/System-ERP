package com.example.backend.modules.production.departement1.productsExterne.controller;

import com.example.backend.modules.production.departement1.productsExterne.dto.IncomingMaterialDTO;
import com.example.backend.modules.production.departement1.productsExterne.entity.IncomingMaterial;
import com.example.backend.modules.production.departement1.shared.entity.StockDep1;
import com.example.backend.modules.production.departement1.productsExterne.service.ProductExterneService;
import com.example.backend.modules.admin.usermanagement.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/psf/externe")
@RequiredArgsConstructor
public class ProductExterneController {

    private final ProductExterneService productExterneService;

    @PostMapping(value = "/incoming", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasRole('LOGISTIC')")
    public ResponseEntity<byte[]> registerIncoming(@RequestBody IncomingMaterialDTO incomingDTO,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(productExterneService.registerIncoming(incomingDTO, user));
    }

    @PutMapping("/incoming/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IncomingMaterial> updateIncoming(@PathVariable Long id,
            @RequestBody IncomingMaterialDTO incomingDTO) {
        return ResponseEntity.ok(productExterneService.updateIncoming(id, incomingDTO));
    }

    @DeleteMapping("/incoming/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteIncoming(@PathVariable Long id) {
        productExterneService.deleteIncoming(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stocks")
    @PreAuthorize("hasAnyRole('LOGISTIC', 'ADMIN')")
    public ResponseEntity<List<StockDep1>> getAllStocks() {
        return ResponseEntity.ok(productExterneService.getAllStocks());
    }

    @PutMapping("/stocks/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StockDep1> updateStock(@PathVariable Long id, @RequestBody StockDep1 stockDTO) {
        return ResponseEntity.ok(productExterneService.updateStock(id, stockDTO));
    }

    @DeleteMapping("/stocks/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStock(@PathVariable Long id) {
        productExterneService.deleteStock(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/incoming")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<List<IncomingMaterial>> getIncomingHistory() {
        return ResponseEntity.ok(productExterneService.getAllIncoming());
    }

    @GetMapping(value = "/qr/generate", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasRole('LOGISTIC')")
    public ResponseEntity<byte[]> generateQRCode(
            @RequestParam String reference,
            @RequestParam Double quantity,
            @RequestParam String lotNumber) {
        return ResponseEntity.ok(productExterneService.generateQRCode(reference, quantity, lotNumber));
    }
}
