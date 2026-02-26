package com.example.backend.modules.admin.simulation.service;

import com.example.backend.modules.admin.simulation.dto.SimulationRequest;
import com.example.backend.modules.admin.simulation.dto.SimulationResponse;
import com.example.backend.modules.admin.nomenclature.entity.Nomenclature;
import com.example.backend.modules.production.productionstock.entity.Product;
import com.example.backend.modules.admin.nomenclature.repository.NomenclatureRepository;
import com.example.backend.modules.production.productionstock.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private final ProductRepository productRepository;
    private final NomenclatureRepository nomenclatureRepository;

    public SimulationResponse checkFeasibility(SimulationRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found: " + request.getProductId()));

        Map<String, Double> requirements = new HashMap<>();
        calculateRequirements(product, request.getQuantity(), requirements);

        StringBuilder missing = new StringBuilder();
        boolean feasible = true;

        for (Map.Entry<String, Double> entry : requirements.entrySet()) {
            String ref = entry.getKey();
            Double required = entry.getValue();

            Product component = productRepository.findByRef(ref).orElse(null);
            double available = (component != null) ? component.getQuantityAvailable() : 0.0;

            if (available < required) {
                feasible = false;
                if (missing.length() > 0)
                    missing.append(", ");
                missing.append(ref).append(" (Need ").append(required).append(", Have ").append(available).append(")");
            }
        }

        if (feasible) {
            return new SimulationResponse(true, "All components are available in stock.");
        } else {
            return new SimulationResponse(false, "Missing components: " + missing.toString());
        }
    }

    private void calculateRequirements(Product product, Double quantity, Map<String, Double> requirements) {
        List<Nomenclature> components = nomenclatureRepository.findByParentProduct(product);

        if (components.isEmpty()) {
            // Leaf node (Raw material or PSF without BOM recorded here)
            // Add to requirements
            requirements.put(product.getRef(), requirements.getOrDefault(product.getRef(), 0.0) + quantity);
            return;
        }

        for (Nomenclature n : components) {
            calculateRequirements(n.getComponentProduct(), quantity * n.getQuantityRequired(), requirements);
        }
    }
}
