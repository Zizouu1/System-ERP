package com.example.backend.modules.production.productionstock.service;

import com.example.backend.modules.production.productionstock.dto.ProductRequest;
import com.example.backend.modules.admin.nomenclature.entity.Nomenclature;
import com.example.backend.modules.production.productionstock.entity.GlobalStock;
import com.example.backend.modules.production.productionstock.entity.ProductionDetail;
import com.example.backend.modules.admin.product.entity.ProductTypeEnum;
import com.example.backend.modules.admin.nomenclature.repository.NomenclatureRepository;
import com.example.backend.modules.production.productionstock.repository.GlobalStockRepository;
// Removed unused ProductRepository import
import com.example.backend.modules.production.productionstock.repository.ProductionDetailRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
// Removed unused Objects import

@Service("productionStockProductService")
@RequiredArgsConstructor
public class ProductService {

    private final GlobalStockRepository globalStockRepository;
    // Removed deprecated ProductRepository field
    private final NomenclatureRepository nomenclatureRepository;
    private final ProductionDetailRepository productionDetailRepository;

    public GlobalStock createProduct(ProductRequest request) {
    GlobalStock product = GlobalStock.builder()
        .ref(request.getRef())
        .type(request.getType()) // Ensure ProductRequest uses ProductTypeEnum
        .quantityTotal(0.0)
        .quantityUsed(0.0)
        .build();
    return globalStockRepository.save(product);
    }

    public List<GlobalStock> getAllProducts() {
        return globalStockRepository.findAll();
    }

    public GlobalStock getProductByRef(String ref) {
        return globalStockRepository.findByRef(ref)
                .orElseThrow(() -> new RuntimeException("Product not found: " + ref));
    }

    public void validateRef(String ref) {
        boolean existsInBom = nomenclatureRepository.existsByParentRef(ref) ||
                nomenclatureRepository.existsByComponentRef(ref);
        if (!existsInBom) {
            throw new RuntimeException(
                    "Reference " + ref + " not found in Nomenclature (BOM) table. Operation denied.");
        }
    }

    private GlobalStock getOrRegisterProduct(String ref) {
    return getOrRegisterProduct(ref, ProductTypeEnum.MATIERE_PREMIERE);
    }

    private GlobalStock getOrRegisterProduct(String ref, ProductTypeEnum type) {
        validateRef(ref);
    return globalStockRepository.findByRef(ref).orElseGet(() -> {
        GlobalStock newProduct = GlobalStock.builder()
            .ref(ref)
            .type(type)
            .quantityTotal(0.0)
            .quantityUsed(0.0)
            .build();
    return globalStockRepository.save(newProduct);
    });
    }

    /**
     * Called by any module when products arrive (incoming).
     * Increases quantityTotal → quantityAvailable goes up.
     */
    @Transactional
    public void increaseQuantity(String ref, double amount) {
        increaseQuantity(ref, amount, ProductTypeEnum.MATIERE_PREMIERE);
    }

    /**
     * Called by any module when products arrive (incoming).
     * Increases quantityTotal → quantityAvailable goes up.
     */
    @Transactional
    public void increaseQuantity(String ref, double amount, ProductTypeEnum type) {
        GlobalStock product = getOrRegisterProduct(ref, type);
        // Update type if product exists and type differs (can be upgraded from MATIERE_PREMIERE to others)
        if (product.getType() != type && product.getType() == ProductTypeEnum.MATIERE_PREMIERE) {
            product.setType(type);
        }
        product.setQuantityTotal(product.getQuantityTotal() + amount);
        globalStockRepository.save(product);
    }

    /**
     * Full production declaration with BOM:
     * 1. Check if parent ref is in Nomenclature
     * 2. For each component: increase quantityUsed (decrease available)
     * 3. For produced product: increase quantityTotal (increase available)
     */
    @Transactional
    public List<ProductionDetail> declareProduction(String reference, double quantity, String productionRef) {
        return declareProduction(reference, quantity, productionRef, ProductTypeEnum.MATIERE_PREMIERE);
    }

    /**
     * Full production declaration with BOM:
     * 1. Check if parent ref is in Nomenclature
     * 2. For each component: increase quantityUsed (decrease available)
     * 3. For produced product: increase quantityTotal (increase available)
     */
    @Transactional
    public List<ProductionDetail> declareProduction(String reference, double quantity, String productionRef, ProductTypeEnum productType) {
        validateRef(reference);
        GlobalStock parentProduct = getOrRegisterProduct(reference, productType);
        // Update type if product exists and type differs (can be upgraded from MATIERE_PREMIERE to others)
        if (parentProduct.getType() != productType && parentProduct.getType() == ProductTypeEnum.MATIERE_PREMIERE) {
            parentProduct.setType(productType);
        }

        // 1. Read BOM (as Strings)
        List<Nomenclature> bom = nomenclatureRepository.findByParentRef(reference);

        if (bom.isEmpty()) {
            throw new RuntimeException("No BOM defined for product: " + reference);
        }

        List<ProductionDetail> details = new ArrayList<>();

        // 2. Consume components
        for (Nomenclature entry : bom) {
            GlobalStock component = getOrRegisterProduct(entry.getComponentRef());
            double consumed = entry.getQuantityRequired() * quantity;

            // Decrease component availability
            component.setQuantityUsed(component.getQuantityUsed() + consumed);
            globalStockRepository.save(component);

            // 4. Save ProductionDetail
            ProductionDetail detail = ProductionDetail.builder()
                    .productionRef(productionRef)
                    .componentProduct(component)
                    .quantityConsumed(consumed)
                    .timestamp(LocalDateTime.now())
                    .build();
            details.add(productionDetailRepository.save(detail));
        }

        // 3. Increase produced product
        parentProduct.setQuantityTotal(parentProduct.getQuantityTotal() + quantity);
        globalStockRepository.save(parentProduct);

        return details;
    }

    public List<ProductionDetail> getProductionDetails(String productionRef) {
        return productionDetailRepository.findByProductionRef(productionRef);
    }

    public List<GlobalStock> getProducibleProducts(ProductTypeEnum type) {
        List<GlobalStock> productsByType = globalStockRepository.findByType(type);
        return productsByType.stream()
                .filter(p -> nomenclatureRepository.existsByParentRef(p.getRef()))
                .toList();
    }

    /**
     * Reverts a production declaration:
     * 1. Restores consumed components (decreases quantityUsed)
     * 2. Decreases produced product's quantityTotal
     * 3. Removes ProductionDetail records
     */
    @Transactional
    public void revertProduction(String reference, double quantity, String productionRef) {
        GlobalStock parentProduct = getProductByRef(reference);

        List<ProductionDetail> details = productionDetailRepository.findByProductionRef(productionRef);
        for (ProductionDetail detail : details) {
            GlobalStock component = detail.getComponentProduct();
            component.setQuantityUsed(component.getQuantityUsed() - detail.getQuantityConsumed());
            globalStockRepository.save(component);
            productionDetailRepository.delete(detail);
        }

        parentProduct.setQuantityTotal(parentProduct.getQuantityTotal() - quantity);
        globalStockRepository.save(parentProduct);
    }
}
