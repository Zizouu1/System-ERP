package com.example.backend.modules.production.productionstock.service;

import com.example.backend.modules.production.productionstock.dto.GlobalStockUpdateRequest;
import com.example.backend.modules.admin.nomenclature.entity.Nomenclature;
import com.example.backend.modules.production.productionstock.entity.GlobalStock;
import com.example.backend.modules.admin.product.entity.ProductTypeEnum;
import com.example.backend.modules.admin.nomenclature.repository.NomenclatureRepository;
import com.example.backend.modules.admin.product.repository.ProductRepository;
import com.example.backend.modules.production.productionstock.repository.GlobalStockRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service("productionStockProductService")
@RequiredArgsConstructor
public class GlobalStockService {

    private final GlobalStockRepository globalStockRepository;
    
    private final NomenclatureRepository nomenclatureRepository;
    private final ProductRepository productRepository;



    public List<GlobalStock> getAllProducts() {
        return globalStockRepository.findAll();
    }

    public GlobalStock getProductByRef(String ref) {
        return globalStockRepository.findByRef(ref)
                .orElseThrow(() -> new RuntimeException("Produit introuvable : " + ref));
    }

    public void validateRef(String ref) {
        boolean existsInProduct = productRepository.existsByRef(ref);
        if (!existsInProduct) {
            throw new RuntimeException(
                    "Référence produit introuvable : " + ref + ". Opération refusée.");
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
    public void declareProduction(String reference, double quantity, String productionRef) {
        declareProduction(reference, quantity, productionRef, ProductTypeEnum.MATIERE_PREMIERE, false);
    }

    /**
     * Full production declaration with BOM:
     * 1. Check if parent ref is in Nomenclature
     * 2. For each component: increase quantityUsed (decrease available)
     * 3. For produced product: increase quantityTotal (increase available)
     */
    @Transactional
    public void declareProduction(String reference, double quantity, String productionRef, ProductTypeEnum productType) {
        declareProduction(reference, quantity, productionRef, productType, false);
    }

    @Transactional
    public void declareProduction(String reference, double quantity, String productionRef, ProductTypeEnum productType, boolean isAdmin) {
        validateRef(reference);
        GlobalStock parentProduct = getOrRegisterProduct(reference, productType);
        // Update type if product exists and type differs (can be upgraded from MATIERE_PREMIERE to others)
        if (parentProduct.getType() != productType && parentProduct.getType() == ProductTypeEnum.MATIERE_PREMIERE) {
            parentProduct.setType(productType);
        }

        // 1. Read BOM (as Strings)
        List<Nomenclature> bom = nomenclatureRepository.findByParentRef(reference);

        if (bom.isEmpty()) {
            throw new RuntimeException("Aucune nomenclature définie pour le produit : " + reference);
        }

        // 2. Consume components
        for (Nomenclature entry : bom) {
            GlobalStock component = getOrRegisterProduct(entry.getComponentRef());
            double consumed = entry.getQuantityRequired() * quantity;

            assertNonNegativeStock(component, consumed, isAdmin);

            // Decrease component availability
            component.setQuantityUsed(component.getQuantityUsed() + consumed);
            globalStockRepository.save(component);

        }

        // 3. Increase produced product
        parentProduct.setQuantityTotal(parentProduct.getQuantityTotal() + quantity);
        globalStockRepository.save(parentProduct);

    }

    private void assertNonNegativeStock(GlobalStock component, double consumed, boolean isAdmin) {
        if (component.getQuantityAvailable() < consumed) {
            String message = isAdmin
                    ? String.format(
                            "Le stock ne peut pas être négatif pour %s (disponible %.2f, requis %.2f)",
                            component.getRef(),
                            component.getQuantityAvailable(),
                            consumed)
                    : String.format(
                            "Stock insuffisant pour %s (disponible %.2f, requis %.2f)",
                            component.getRef(),
                            component.getQuantityAvailable(),
                            consumed);
            throw new IllegalArgumentException(message);
        }
    }

    public List<GlobalStock> getProducibleProducts(ProductTypeEnum type) {
        List<GlobalStock> productsByType = globalStockRepository.findByType(type);
        return productsByType.stream()
                .filter(p -> nomenclatureRepository.existsByParentRef(p.getRef()))
                .toList();
    }

    @Transactional
    public GlobalStock updateGlobalStock(Long id, GlobalStockUpdateRequest request) {
        GlobalStock stock = globalStockRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock global introuvable."));

        Double newTotal = request.getQuantityTotal() != null ? request.getQuantityTotal() : stock.getQuantityTotal();
        Double newUsed = request.getQuantityUsed() != null ? request.getQuantityUsed() : stock.getQuantityUsed();

        if (newTotal < 0 || newUsed < 0) {
            throw new IllegalArgumentException("Les quantités ne peuvent pas être négatives.");
        }
        if (newUsed > newTotal) {
            throw new IllegalArgumentException("La quantité utilisée ne peut pas dépasser la quantité totale.");
        }

        stock.setQuantityTotal(newTotal);
        stock.setQuantityUsed(newUsed);
        return globalStockRepository.save(stock);
    }

    @Transactional
    public void deleteGlobalStock(Long id) {
        GlobalStock stock = globalStockRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock global introuvable."));
        globalStockRepository.delete(stock);
    }

    /**
     * Reverts a production declaration:
     * 1. Restores consumed components (decreases quantityUsed)
     * 2. Decreases produced product's quantityTotal
    * 3. Recomputes component consumption from BOM
     */
    @Transactional
    public void revertProduction(String reference, double quantity, String productionRef) {
        GlobalStock parentProduct = getProductByRef(reference);

        List<Nomenclature> bom = nomenclatureRepository.findByParentRef(reference);
        for (Nomenclature entry : bom) {
            GlobalStock component = getOrRegisterProduct(entry.getComponentRef());
            double consumed = entry.getQuantityRequired() * quantity;
            component.setQuantityUsed(component.getQuantityUsed() - consumed);
            globalStockRepository.save(component);
        }

        parentProduct.setQuantityTotal(parentProduct.getQuantityTotal() - quantity);
        globalStockRepository.save(parentProduct);
    }
}
