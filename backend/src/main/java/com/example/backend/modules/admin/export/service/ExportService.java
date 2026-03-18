package com.example.backend.modules.admin.export.service;

import com.example.backend.modules.admin.export.dto.ExportRequest;
import com.example.backend.modules.admin.export.entity.Export;
import com.example.backend.modules.admin.export.repository.ExportRepository;
import com.example.backend.modules.admin.nomenclature.entity.Nomenclature;
import com.example.backend.modules.production.productionstock.entity.GlobalStock;
import com.example.backend.modules.admin.product.service.ProductService;
import com.example.backend.modules.admin.nomenclature.repository.NomenclatureRepository;
import com.example.backend.modules.production.productionstock.repository.GlobalStockRepository;
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
    private final GlobalStockRepository globalStockRepository;
    private final NomenclatureRepository nomenclatureRepository;
    private final ProductService masterProductService;

    public List<Export> getAllExports() {
        return exportRepository.findAllByOrderByExportDateDesc();
    }

    @Transactional
    public Export createExport(ExportRequest request) {
        if (request.getQuantity() <= 0) {
            throw new IllegalArgumentException("La quantité d'exportation doit être positive");
        }

        // Validate product reference exists in Product master
        masterProductService.validateProductReference(request.getReference());

        GlobalStock product = globalStockRepository.findByRef(request.getReference())
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable : " + request.getReference()));

        // 1. Validate Availability (total - used >= requested)
        if (product.getQuantityAvailable() < request.getQuantity()) {
            throw new IllegalArgumentException("Stock insuffisant pour le produit " + product.getRef() +
                    ". Disponible : " + product.getQuantityAvailable() + ", Requis : " + request.getQuantity());
        }

        // 2. Explode BOM Recursively and Update Component Stocks
        recursiveBOMStockUpdate(product.getRef(), request.getQuantity(), false);

        // 3. Update Finished Product Stock
        // Rule: Decrease totalQuantity by export quantity. Do NOT modify usedQuantity.
        product.setQuantityTotal(product.getQuantityTotal() - request.getQuantity());
        globalStockRepository.save(product);

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
                .orElseThrow(() -> new EntityNotFoundException("Export introuvable : " + id));

        // 1. Reverse previous export (add back quantities)
        recursiveBOMStockUpdate(existingExport.getProduct().getRef(), existingExport.getQuantity(), true);

        GlobalStock product = existingExport.getProduct();
        product.setQuantityTotal(product.getQuantityTotal() + existingExport.getQuantity());
        globalStockRepository.save(product);

        // 2. Validate new request availability (after reversal)
        // If product changed, we need to fetch the new one
        if (!existingExport.getProduct().getRef().equals(request.getReference())) {
            // Validate product reference exists in Product master
            masterProductService.validateProductReference(request.getReference());
            product = globalStockRepository.findByRef(request.getReference())
                    .orElseThrow(() -> new EntityNotFoundException("Produit introuvable : " + request.getReference()));
        }

        if (product.getQuantityAvailable() < request.getQuantity()) {
            throw new IllegalArgumentException("Stock insuffisant pour le produit " + product.getRef());
        }

        // 3. Apply new export
        recursiveBOMStockUpdate(product.getRef(), request.getQuantity(), false);
        product.setQuantityTotal(product.getQuantityTotal() - request.getQuantity());
        globalStockRepository.save(product);

        existingExport.setProduct(product);
        existingExport.setQuantity(request.getQuantity());

        return exportRepository.save(existingExport);
    }

    @Transactional
    public void deleteExport(Long id) {
        Export export = exportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Export not found: " + id));

        // Reverse stock changes
        recursiveBOMStockUpdate(export.getProduct().getRef(), export.getQuantity(), true);

        GlobalStock product = export.getProduct();
        product.setQuantityTotal(product.getQuantityTotal() + export.getQuantity());
        globalStockRepository.save(product);

        exportRepository.delete(export);
    }

    /**
     * Recursively traverses the BOM and updates component stocks.
     * 
     * @param parentRef      The reference of the product whose BOM we are
     *                       traversing.
     * @param parentQuantity The quantity of the parent product being exported.
     * @param isReversal     If true, we ADD back to stock (reverse export). If
     *                       false, we SUBTRACT.
     */
    private void recursiveBOMStockUpdate(String parentRef, double parentQuantity, boolean isReversal) {
        List<Nomenclature> bom = nomenclatureRepository.findByParentRef(parentRef);

        for (Nomenclature entry : bom) {
            String componentRef = entry.getComponentRef();
            double requiredQuantity = entry.getQuantityRequired() * parentQuantity;

            // Recurse first (depth-first)
            recursiveBOMStockUpdate(componentRef, requiredQuantity, isReversal);

            // Update Component Stock
            GlobalStock component = globalStockRepository.findByRef(componentRef)
                    .orElseThrow(() -> new EntityNotFoundException("Component stock record missing: " + componentRef));

            // Rule: Decrease/Increase BOTH totalQuantity and usedQuantity
            if (isReversal) {
                component.setQuantityTotal(component.getQuantityTotal() + requiredQuantity);
                component.setQuantityUsed(component.getQuantityUsed() + requiredQuantity);
            } else {
                if (component.getQuantityTotal() < requiredQuantity) {
                    throw new IllegalArgumentException("Data integrity error: Component " + component.getRef() +
                            " has insufficient total quantity during BOM explosion.");
                }
                component.setQuantityTotal(component.getQuantityTotal() - requiredQuantity);
                component.setQuantityUsed(component.getQuantityUsed() - requiredQuantity);
            }
            globalStockRepository.save(component);
        }
    }
}
