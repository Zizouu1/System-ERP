package com.example.backend.modules.admin.export.service;

import com.example.backend.modules.admin.export.dto.ExportRequest;
import com.example.backend.modules.admin.export.entity.Export;
import com.example.backend.modules.admin.export.repository.ExportRepository;
import com.example.backend.modules.admin.nomenclature.entity.Nomenclature;
import com.example.backend.modules.production.productionstock.entity.Product;
import com.example.backend.modules.admin.nomenclature.repository.NomenclatureRepository;
import com.example.backend.modules.production.productionstock.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final ExportRepository exportRepository;
    private final ProductRepository productRepository;
    private final NomenclatureRepository nomenclatureRepository;

    public List<Export> getAllExports() {
        return exportRepository.findAllByOrderByExportDateDesc();
    }

    @Transactional
    public Export createExport(ExportRequest request) {
        if (request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Export quantity must be positive");
        }

        Product product = productRepository.findByRef(request.getReference())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + request.getReference()));

        // 1. Validate Availability (total - used >= requested)
        if (product.getQuantityAvailable() < request.getQuantity()) {
            throw new IllegalArgumentException("Insufficient stock for product " + product.getRef() +
                    ". Available: " + product.getQuantityAvailable() + ", Requested: " + request.getQuantity());
        }

        // 2. Explode BOM Recursively and Update Component Stocks
        recursiveBOMStockUpdate(product, request.getQuantity(), false);

        // 3. Update Finished Product Stock
        // Rule: Decrease totalQuantity by export quantity. Do NOT modify usedQuantity.
        product.setQuantityTotal(product.getQuantityTotal() - request.getQuantity());
        productRepository.save(product);

        // 4. Save Export Record
        Export export = Export.builder()
                .product(product)
                .quantity(request.getQuantity())
                .exportDate(LocalDateTime.now())
                .build();
        return exportRepository.save(export);
    }

    @Transactional
    public Export updateExport(Long id, ExportRequest request) {
        Export existingExport = exportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Export not found: " + id));

        // 1. Reverse previous export (add back quantities)
        recursiveBOMStockUpdate(existingExport.getProduct(), existingExport.getQuantity(), true);

        Product product = existingExport.getProduct();
        product.setQuantityTotal(product.getQuantityTotal() + existingExport.getQuantity());
        productRepository.save(product);

        // 2. Validate new request availability (after reversal)
        // If product changed, we need to fetch the new one
        if (!existingExport.getProduct().getRef().equals(request.getReference())) {
            product = productRepository.findByRef(request.getReference())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found: " + request.getReference()));
        }

        if (product.getQuantityAvailable() < request.getQuantity()) {
            throw new IllegalArgumentException("Insufficient stock for product " + product.getRef());
        }

        // 3. Apply new export
        recursiveBOMStockUpdate(product, request.getQuantity(), false);
        product.setQuantityTotal(product.getQuantityTotal() - request.getQuantity());
        productRepository.save(product);

        existingExport.setProduct(product);
        existingExport.setQuantity(request.getQuantity());

        return exportRepository.save(existingExport);
    }

    @Transactional
    public void deleteExport(Long id) {
        Export export = exportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Export not found: " + id));

        // Reverse stock changes
        recursiveBOMStockUpdate(export.getProduct(), export.getQuantity(), true);

        Product product = export.getProduct();
        product.setQuantityTotal(product.getQuantityTotal() + export.getQuantity());
        productRepository.save(product);

        exportRepository.delete(export);
    }

    /**
     * Recursively traverses the BOM and updates component stocks.
     * 
     * @param parentProduct  The product whose BOM we are traversing.
     * @param parentQuantity The quantity of the parent product being exported.
     * @param isReversal     If true, we ADD back to stock (reverse export). If
     *                       false, we SUBTRACT.
     */
    private void recursiveBOMStockUpdate(Product parentProduct, double parentQuantity, boolean isReversal) {
        List<Nomenclature> bom = nomenclatureRepository.findByParentProduct(parentProduct);

        for (Nomenclature entry : bom) {
            Product component = entry.getComponentProduct();
            double requiredQuantity = entry.getQuantityRequired() * parentQuantity;

            // Recurse first (depth-first)
            recursiveBOMStockUpdate(component, requiredQuantity, isReversal);

            // Update Component Stock
            // Rule: Decrease/Increase BOTH totalQuantity and usedQuantity
            if (isReversal) {
                component.setQuantityTotal(component.getQuantityTotal() + requiredQuantity);
                component.setQuantityUsed(component.getQuantityUsed() + requiredQuantity);
            } else {
                if (component.getQuantityTotal() < requiredQuantity) {
                    // Should strictly not happen if parent was available and logic is sound,
                    // but good safety check if database is inconsistent
                    throw new IllegalArgumentException("Data integrity error: Component " + component.getRef() +
                            " has insufficient total quantity during BOM explosion.");
                }
                component.setQuantityTotal(component.getQuantityTotal() - requiredQuantity);
                component.setQuantityUsed(component.getQuantityUsed() - requiredQuantity);
            }
            productRepository.save(component);
        }
    }
}
