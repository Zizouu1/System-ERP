package com.example.backend.modules.production.productionstock.service;

import com.example.backend.modules.production.productionstock.dto.ProductRequest;
import com.example.backend.modules.admin.nomenclature.entity.Nomenclature;
import com.example.backend.modules.production.productionstock.entity.Product;
import com.example.backend.modules.production.productionstock.entity.ProductionDetail;
import com.example.backend.modules.production.productionstock.entity.ProductType;
import com.example.backend.modules.admin.nomenclature.repository.NomenclatureRepository;
import com.example.backend.modules.production.productionstock.repository.ProductRepository;
import com.example.backend.modules.production.productionstock.repository.ProductionDetailRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final NomenclatureRepository nomenclatureRepository;
    private final ProductionDetailRepository productionDetailRepository;

    public Product createProduct(ProductRequest request) {
        Product product = Product.builder()
                .ref(request.getRef())
                .type(request.getType())
                .quantityTotal(0.0)
                .quantityUsed(0.0)
                .build();
        return productRepository.save(product);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductByRef(String ref) {
        return productRepository.findByRef(ref)
                .orElseThrow(() -> new RuntimeException("Product not found: " + ref));
    }

    /**
     * Called by any module when products arrive (incoming).
     * Increases quantityTotal → quantityAvailable goes up.
     */
    @Transactional
    public void increaseQuantity(String ref, double amount) {
        Product product = getProductByRef(ref);
        product.setQuantityTotal(product.getQuantityTotal() + amount);
        productRepository.save(product);
    }

    /**
     * Full production declaration with BOM:
     * 1. Read BOM for the product
     * 2. For each component: increase quantityUsed (decrease available)
     * 3. For produced product: increase quantityTotal (increase available)
     * 4. Save ProductionDetail for each consumed component
     *
     * @param reference     reference of the product being produced
     * @param quantity      how many units produced
     * @param productionRef unique ID linking details to the production record
     * @return list of ProductionDetail records saved
     */
    @Transactional
    public List<ProductionDetail> declareProduction(String reference, double quantity, String productionRef) {
        Product parentProduct = getProductByRef(reference);

        // 1. Read BOM
        List<Nomenclature> bom = nomenclatureRepository.findByParentProduct(parentProduct);

        List<ProductionDetail> details = new ArrayList<>();

        // 2. Consume components
        for (Nomenclature entry : bom) {
            Product component = entry.getComponentProduct();
            double consumed = entry.getQuantityRequired() * quantity;

            // Decrease component availability
            component.setQuantityUsed(component.getQuantityUsed() + consumed);
            productRepository.save(component);

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
        productRepository.save(parentProduct);

        return details;
    }

    public List<ProductionDetail> getProductionDetails(String productionRef) {
        return productionDetailRepository.findByProductionRef(productionRef);
    }

    public List<Product> getProducibleProducts(ProductType type) {
        List<Product> productsByType = productRepository.findByType(type);
        return productsByType.stream()
                .filter(p -> !nomenclatureRepository.findByParentProduct(p).isEmpty())
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
        Product parentProduct = getProductByRef(reference);

        List<ProductionDetail> details = productionDetailRepository.findByProductionRef(productionRef);
        for (ProductionDetail detail : details) {
            Product component = detail.getComponentProduct();
            component.setQuantityUsed(component.getQuantityUsed() - detail.getQuantityConsumed());
            productRepository.save(component);
            productionDetailRepository.delete(detail);
        }

        parentProduct.setQuantityTotal(parentProduct.getQuantityTotal() - quantity);
        productRepository.save(parentProduct);
    }
}
