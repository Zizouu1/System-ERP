package com.example.backend.modules.production.shared.service;

import com.example.backend.modules.production.shared.dto.ProductRequest;
import com.example.backend.modules.production.shared.entity.Nomenclature;
import com.example.backend.modules.production.shared.entity.Product;
import com.example.backend.modules.production.shared.entity.ProductionDetail;
import com.example.backend.modules.production.shared.entity.ProductType;
import com.example.backend.modules.production.shared.repository.NomenclatureRepository;
import com.example.backend.modules.production.shared.repository.ProductRepository;
import com.example.backend.modules.production.shared.repository.ProductionDetailRepository;
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
     * @param productRef    reference of the product being produced
     * @param quantity      how many units produced
     * @param productionRef unique ID linking details to the production record
     * @return list of ProductionDetail records saved
     */
    @Transactional
    public List<ProductionDetail> declareProduction(String productRef, double quantity, String productionRef) {
        Product parentProduct = getProductByRef(productRef);

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
}
