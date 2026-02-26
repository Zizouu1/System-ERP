package com.example.backend.modules.production.productionstock.controller;

import com.example.backend.modules.production.productionstock.dto.ProductRequest;
import com.example.backend.modules.production.productionstock.dto.ProductionDeclarationRequest;
import com.example.backend.modules.production.productionstock.entity.Product;
import com.example.backend.modules.production.productionstock.entity.ProductType;
import com.example.backend.modules.production.productionstock.entity.ProductionDetail;
import com.example.backend.modules.production.productionstock.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("denyAll()") // "ADMIN cannot create directly from this table"
    public ResponseEntity<Product> createProduct(@RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.createProduct(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{ref}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> getProductByRef(@PathVariable String ref) {
        return ResponseEntity.ok(productService.getProductByRef(ref));
    }

    @PostMapping("/produce")
    @PreAuthorize("denyAll()") // Product production goes through specific departments
    public ResponseEntity<List<ProductionDetail>> declareProduction(@RequestBody ProductionDeclarationRequest request) {
        List<ProductionDetail> details = productService.declareProduction(
                request.getReference(),
                request.getQuantity(),
                "DIRECT-" + System.currentTimeMillis());
        return ResponseEntity.ok(details);
    }

    @GetMapping("/production-details/{productionRef}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductionDetail>> getProductionDetails(@PathVariable String productionRef) {
        return ResponseEntity.ok(productService.getProductionDetails(productionRef));
    }

    @GetMapping("/producible")
    @PreAuthorize("hasAnyRole('ADMIN', 'PF', 'PSF')")
    public ResponseEntity<List<Product>> getProducibleProducts(@RequestParam ProductType type) {
        return ResponseEntity.ok(productService.getProducibleProducts(type));
    }
}