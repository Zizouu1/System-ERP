package com.example.backend.modules.admin.product.controller;

import com.example.backend.modules.admin.product.dto.ProductRequest;
import com.example.backend.modules.admin.product.dto.ProductResponse;
import com.example.backend.modules.admin.product.entity.ProductTypeEnum;
import com.example.backend.modules.admin.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Product Master Management Controller
 * Endpoints for Product master data:
 * - CRUD operations for admin
 * - Read/validation endpoints for authenticated roles that depend on product references
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(@RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.createProduct(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LOGISTIC', 'PSF', 'PF')")
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAnyRole('ADMIN', 'LOGISTIC', 'PSF', 'PF')")
    public ResponseEntity<Page<ProductResponse>> getAllProductsPaged(Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProductsPaged(pageable));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'LOGISTIC', 'PSF', 'PF')")
    public ResponseEntity<List<ProductResponse>> getActiveProducts() {
        return ResponseEntity.ok(productService.getActiveProducts());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LOGISTIC', 'PSF', 'PF')")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/ref/{ref}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LOGISTIC', 'PSF', 'PF')")
    public ResponseEntity<ProductResponse> getProductByRef(@PathVariable String ref) {
        return ResponseEntity.ok(productService.getProductByRef(ref));
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LOGISTIC', 'PSF', 'PF')")
    public ResponseEntity<List<ProductResponse>> getProductsByType(@PathVariable ProductTypeEnum type) {
        return ResponseEntity.ok(productService.getProductsByType(type));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'LOGISTIC', 'PSF', 'PF')")
    public ResponseEntity<List<ProductResponse>> searchProducts(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(productService.searchProducts(q));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id, @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Bulk import products from CSV data
     */
    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> importCSV(@RequestBody List<String[]> rows) {
        return ResponseEntity.ok(productService.importCSV(rows));
    }

    /**
     * Validate that a product reference exists
     */
    @GetMapping("/validate/{ref}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LOGISTIC', 'PSF', 'PF')")
    public ResponseEntity<Boolean> validateRef(@PathVariable String ref) {
        return ResponseEntity.ok(productService.refExists(ref));
    }
}
