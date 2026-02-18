package com.example.backend.modules.production.simulation.service;

import com.example.backend.modules.production.shared.entity.*;
import com.example.backend.modules.production.simulation.dto.*;
import com.example.backend.modules.production.shared.repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductionSimulationService {

    private final ProductRepository productRepository;
    private final BOMRepository bomRepository;
    private final StockRepository stockRepository;

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

            Stock stock = stockRepository.findByProduct(component)
                    .orElseThrow(() -> new RuntimeException("Stock not found"));

            Double availableQty = stock.getAvailableQuantity();

            if (requiredQty > availableQty) {

                Double missingQty = requiredQty - availableQty;

                missingItems.add(
                        new MissingItem(
                                component.getReference(),
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

        List<BOM> components = bomRepository.findByParentProduct(product);

        // If no children → raw material
        if (components.isEmpty()) {
            totalRequired.merge(product, quantity, Double::sum);
            return;
        }

        // If has children → explode each component
        for (BOM bom : components) {

            Product component = bom.getComponentProduct();
            Double requiredQty = quantity * bom.getQuantityRequired();

            explode(component, requiredQty, totalRequired);
        }
    }
}
