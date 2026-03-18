package com.example.backend.modules.admin.nomenclature.service;

import com.example.backend.modules.admin.nomenclature.dto.NomenclatureRequest;
import com.example.backend.modules.admin.nomenclature.entity.Nomenclature;
import com.example.backend.modules.admin.nomenclature.repository.NomenclatureRepository;
import com.example.backend.modules.admin.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class NomenclatureService {

    private final NomenclatureRepository nomenclatureRepository;
    private final ProductService productService;

    /**
     * Validates that both parentRef and componentRef exist in the Product master table.
     * Throws exception if either reference is not found.
     */
    private void validateProductReferences(String parentRef, String componentRef) {
        productService.validateProductReference(parentRef);
        productService.validateProductReference(componentRef);
    }

    public Nomenclature createNomenclature(NomenclatureRequest request) {
        // Validate both refs exist in Product master
        validateProductReferences(request.getParentRef(), request.getComponentRef());

        Nomenclature nomenclature = Nomenclature.builder()
                .parentRef(request.getParentRef())
                .componentRef(request.getComponentRef())
                .quantityRequired(request.getQuantityRequired())
                .build();

        return nomenclatureRepository.save(nomenclature);
    }

    public Page<Nomenclature> getAllNomenclatures(Pageable pageable) {
        return nomenclatureRepository.findAll(pageable);
    }

    public List<Nomenclature> getNomenclatureByParentRef(String parentRef) {
        return nomenclatureRepository.findByParentRef(parentRef);
    }

    public Nomenclature updateNomenclature(Long id, Double quantityRequired) {
        Nomenclature nomenclature = nomenclatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nomenclature not found: " + id));
        nomenclature.setQuantityRequired(quantityRequired);
        return nomenclatureRepository.save(nomenclature);
    }

    public void deleteNomenclature(Long id) {
        nomenclatureRepository.deleteById(id);
    }

    public List<String> getUniqueRefs() {
        return nomenclatureRepository.findUniqueRefs();
    }

    public Long countAllNomenclatures() {
        return nomenclatureRepository.count();
    }

    public String importCSV(List<String[]> rows) {
        int added = 0;
        int failed = 0;
        StringBuilder errors = new StringBuilder();

        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row.length < 3) {
                failed++;
                errors.append("Ligne ").append(i + 1).append(": Colonnes insuffisantes\n");
                continue;
            }
            try {
                String parent = row[0].trim();
                String component = row[1].trim();
                double qty = Double.parseDouble(row[2].trim());

                // Validate both references exist in Product master
                validateProductReferences(parent, component);

                Nomenclature nomenclature = nomenclatureRepository
                        .findByParentRefAndComponentRef(parent, component)
                        .orElse(null);

                if (nomenclature != null) {
                    nomenclature.setQuantityRequired(qty);
                } else {
                    nomenclature = Nomenclature.builder()
                            .parentRef(parent)
                            .componentRef(component)
                            .quantityRequired(qty)
                            .build();
                }
                nomenclatureRepository.save(nomenclature);
                added++;
            } catch (Exception e) {
                failed++;
                errors.append("Ligne ").append(i + 1).append(": ").append(e.getMessage()).append("\n");
            }
        }

        return String.format("Succès: %d lignes ajoutées. Échecs: %d.\n%s", added, failed, errors.toString());
    }
}
