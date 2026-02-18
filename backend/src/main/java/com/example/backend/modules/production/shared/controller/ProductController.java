package com.example.backend.modules.production.shared.controller;

import com.example.backend.modules.production.shared.dto.ProductRequest;
import com.example.backend.modules.production.shared.dto.ProductionDeclarationRequest;
import com.example.backend.modules.production.shared.entity.Product;
import com.example.backend.modules.production.shared.entity.ProductType;
import com.example.backend.modules.production.shared.entity.ProductionDetail;
import com.example.backend.modules.production.shared.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.createProduct(request));
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{ref}")
    public ResponseEntity<Product> getProductByRef(@PathVariable String ref) {
        return ResponseEntity.ok(productService.getProductByRef(ref));
    }

    @PostMapping("/produce")
    public ResponseEntity<List<ProductionDetail>> declareProduction(@RequestBody ProductionDeclarationRequest request) {
        List<ProductionDetail> details = productService.declareProduction(
                request.getProductRef(),
                request.getQuantity(),
                "DIRECT-" + System.currentTimeMillis());
        return ResponseEntity.ok(details);
    }

    @GetMapping("/production-details/{productionRef}")
    public ResponseEntity<List<ProductionDetail>> getProductionDetails(@PathVariable String productionRef) {
        return ResponseEntity.ok(productService.getProductionDetails(productionRef));
    }

    @GetMapping("/producible")
    public ResponseEntity<List<Product>> getProducibleProducts(@RequestParam ProductType type) {
        return ResponseEntity.ok(productService.getProducibleProducts(type));
    }
}