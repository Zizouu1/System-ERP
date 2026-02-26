package com.example.backend.modules.admin.simulation.service;

import com.example.backend.modules.production.productionstock.entity.*;
import com.example.backend.modules.admin.simulation.dto.*;
import com.example.backend.modules.admin.nomenclature.repository.*;
import com.example.backend.modules.admin.nomenclature.entity.*;
import com.example.backend.modules.production.productionstock.repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductionSimulationService {

    private final ProductRepository productRepository;
    private final NomenclatureRepository nomenclatureRepository;

    public SimulationResult simulate(List<SimulationRequest> requests) {

        Map<Product, Double> totalRequired = new HashMap<>();

        // 🔁 Explode all selected products
        for (SimulationRequest request : requests) {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            explode(product, request.getQuantity(), totalRequired);
        }

        // 🔎 Compare with stock
        List<MissingItem> missingItems = new ArrayList<>();

        for (Map.Entry<Product, Double> entry : totalRequired.entrySet()) {

            Product component = entry.getKey();
            Double requiredQty = entry.getValue();

            Double availableQty = component.getQuantityAvailable();

            if (requiredQty > availableQty) {

                Double missingQty = requiredQty - availableQty;

                missingItems.add(
                        new MissingItem(
                                component.getRef(),
                                requiredQty,
                                availableQty,
                                missingQty));
            }
        }

        boolean possible = missingItems.isEmpty();

        return new SimulationResult(possible, missingItems);
    }

    // 🔥 Recursive BOM Explosion
    private void explode(Product product,
            Double quantity,
            Map<Product, Double> totalRequired) {

        List<Nomenclature> components = nomenclatureRepository.findByParentProduct(product);

        // If no children → raw material
        if (components.isEmpty()) {
            totalRequired.merge(product, quantity, Double::sum);
            return;
        }

        // If has children → explode each component
        for (Nomenclature bom : components) {

            Product component = bom.getComponentProduct();
            Double requiredQty = quantity * bom.getQuantityRequired();

            explode(component, requiredQty, totalRequired);
        }
    }
}
