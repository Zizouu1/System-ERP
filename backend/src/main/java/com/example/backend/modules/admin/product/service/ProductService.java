
package com.example.backend.modules.admin.product.service;
import org.springframework.lang.NonNull;


import com.example.backend.modules.admin.product.dto.ProductRequest;
import com.example.backend.modules.admin.product.dto.ProductResponse;
import com.example.backend.modules.admin.product.entity.Product;
import com.example.backend.modules.admin.product.entity.ProductTypeEnum;
import com.example.backend.modules.admin.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    /**
     * Throws if the product reference does not exist in Product master.
     */
    public void validateProductReference(String ref) {
        if (!refExists(ref)) {
            throw new IllegalArgumentException("Le produit '" + ref + "' n'existe pas.");
        }
    }

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse createProduct(@NonNull ProductRequest request) {
        // Validate that ref is unique
        if (productRepository.findByRef(request.getRef()).isPresent()) {
            throw new IllegalArgumentException("Un produit avec cette référence existe déjà: " + request.getRef());
        }

        Product product = Product.builder()
                .ref(request.getRef())
                .designation(request.getDesignation())
                .productType(request.getProductType())
                .description(request.getDescription())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        Product saved = productRepository.save(product);
        return mapToResponse(saved);
    }

    public ProductResponse getProductById(@NonNull Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé: " + id));
        return mapToResponse(product);
    }

    public ProductResponse getProductByRef(String ref) {
        Product product = productRepository.findByRef(ref)
            .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé: " + ref));
        return mapToResponse(product);
    }

    public Page<ProductResponse> getAllProductsPaged(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::mapToResponse);
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<ProductResponse> getActiveProducts() {
        return productRepository.findByActive(true).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<ProductResponse> getProductsByType(ProductTypeEnum productType) {
        return productRepository.findByProductType(productType).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<ProductResponse> searchProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllProducts();
        }
        return productRepository.findByRefContainingIgnoreCaseOrDesignationContainingIgnoreCase(query, query).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public ProductResponse updateProduct(@NonNull Long id, @NonNull ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé: " + id));

        // Check if ref is being changed and if new ref already exists
        if (!product.getRef().equals(request.getRef()) && productRepository.findByRef(request.getRef()).isPresent()) {
            throw new IllegalArgumentException("Un produit avec cette référence existe déjà: " + request.getRef());
        }

        product.setRef(request.getRef());
        product.setDesignation(request.getDesignation());
        product.setProductType(request.getProductType());
        product.setDescription(request.getDescription());
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }

        Product updated = productRepository.save(product);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteProduct(@NonNull Long id) {
        if (!productRepository.existsById(id)) {
            throw new IllegalArgumentException("Produit non trouvé: " + id);
        }
        productRepository.deleteById(id);
    }

    /**
     * Validate that a product reference exists
     */
    public boolean refExists(String ref) {
        return productRepository.findByRef(ref).isPresent();
    }

    @Transactional
    public String importCSV(List<String[]> rows) {
        int added = 0;
        int failed = 0;
        StringBuilder errors = new StringBuilder();

        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row.length < 3) {
                failed++;
                errors.append("Ligne ").append(i + 1).append(": Champs insuffisants (ref, designation, type requis)\n");
                continue;
            }

            try {
                String ref = row[0].trim();
                String designation = row[1].trim();
                String typeStr = row[2].trim().toUpperCase().replace(" ", "_").replace("-", "_");
                String description = row.length > 3 ? row[3].trim() : "";
                // Support both "true"/"false" and "oui"/"non"
                String activeStr = row.length > 4 ? row[4].trim().toLowerCase() : "true";
                boolean active = activeStr.equals("true") || activeStr.equals("oui") || activeStr.equals("yes") || activeStr.equals("1");

                if (ref.isEmpty() || designation.isEmpty()) {
                    throw new IllegalArgumentException("La référence et la désignation sont obligatoires.");
                }

                ProductTypeEnum typeEnum;
                if (typeStr.contains("MATIERE") || typeStr.contains("PREMIERE")) {
                    typeEnum = ProductTypeEnum.MATIERE_PREMIERE;
                } else if (typeStr.contains("SEMI")) {
                    typeEnum = ProductTypeEnum.SEMI_FINI;
                } else if (typeStr.contains("PRODUIT_FINI") || (typeStr.contains("FINI") && !typeStr.contains("SEMI"))) {
                    typeEnum = ProductTypeEnum.PRODUIT_FINI;
                } else {
                    try {
                        typeEnum = ProductTypeEnum.valueOf(typeStr);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Type de produit invalide: " + typeStr);
                    }
                }

                Product product = productRepository.findByRef(ref).orElse(null);
                if (product != null) {
                    product.setDesignation(designation);
                    product.setProductType(typeEnum);
                    product.setDescription(description);
                    product.setActive(active);
                } else {
                    product = Product.builder()
                            .ref(ref)
                            .designation(designation)
                            .productType(typeEnum)
                            .description(description)
                            .active(active)
                            .build();
                }
                productRepository.save(product);
                added++;
            } catch (Exception e) {
                failed++;
                errors.append("Ligne ").append(i + 1).append(": ").append(e.getMessage()).append("\n");
            }
        }

        return String.format("Succès: %d produits importés/mis à jour. Échecs: %d.\n%s", added, failed, errors.toString());
    }

    private ProductResponse mapToResponse(@NonNull Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .ref(product.getRef())
                .designation(product.getDesignation())
                .productType(product.getProductType())
                .displayType(product.getProductType().getDisplayName())
                .description(product.getDescription())
                .active(product.getActive())
                .build();
    }
}
