package com.example.backend.modules.production.shared.service;

import com.example.backend.modules.production.shared.dto.NomenclatureRequest;
import com.example.backend.modules.production.shared.entity.Nomenclature;
import com.example.backend.modules.production.shared.entity.Product;
import com.example.backend.modules.production.shared.repository.NomenclatureRepository;
import com.example.backend.modules.production.shared.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NomenclatureService {

    private final NomenclatureRepository nomenclatureRepository;
    private final ProductRepository productRepository;

    public Nomenclature createNomenclature(NomenclatureRequest request) {
        Product parent = productRepository.findByRef(request.getParentProductRef())
                .orElseThrow(() -> new RuntimeException("Parent product not found: " + request.getParentProductRef()));

        Product component = productRepository.findByRef(request.getComponentProductRef())
                .orElseThrow(
                        () -> new RuntimeException("Component product not found: " + request.getComponentProductRef()));

        Nomenclature nomenclature = Nomenclature.builder()
                .parentProduct(parent)
                .componentProduct(component)
                .quantityRequired(request.getQuantityRequired())
                .build();

        return nomenclatureRepository.save(nomenclature);
    }

    public List<Nomenclature> getAllNomenclatures() {
        return nomenclatureRepository.findAll();
    }

    public List<Nomenclature> getNomenclatureByParentRef(String parentRef) {
        Product parent = productRepository.findByRef(parentRef)
                .orElseThrow(() -> new RuntimeException("Product not found: " + parentRef));
        return nomenclatureRepository.findByParentProduct(parent);
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
}
