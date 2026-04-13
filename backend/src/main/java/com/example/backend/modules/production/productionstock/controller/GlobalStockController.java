package com.example.backend.modules.production.productionstock.controller;

import com.example.backend.modules.production.productionstock.dto.GlobalStockUpdateRequest;
import com.example.backend.modules.production.productionstock.entity.GlobalStock;
import com.example.backend.modules.admin.product.entity.ProductTypeEnum;
import com.example.backend.modules.production.productionstock.service.GlobalStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController("productionStockProductController")
@RequestMapping("/api/production-stock/products")
@RequiredArgsConstructor
public class GlobalStockController {

    private final GlobalStockService productService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GlobalStock>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @PutMapping("/record/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalStock> updateGlobalStock(@PathVariable Long id,
            @RequestBody GlobalStockUpdateRequest request) {
        return ResponseEntity.ok(productService.updateGlobalStock(id, request));
    }

    @DeleteMapping("/record/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteGlobalStock(@PathVariable Long id) {
        productService.deleteGlobalStock(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/producible")
    @PreAuthorize("hasAnyRole('ADMIN', 'PF', 'PSF')")
    public ResponseEntity<List<GlobalStock>> getProducibleProducts(@RequestParam ProductTypeEnum type) {
        return ResponseEntity.ok(productService.getProducibleProducts(type));
    }
}
