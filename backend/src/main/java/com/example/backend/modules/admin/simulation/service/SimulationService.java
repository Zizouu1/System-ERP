package com.example.backend.modules.admin.simulation.service;

import com.example.backend.modules.admin.simulation.dto.SimulationRequest;
import com.example.backend.modules.admin.simulation.dto.SimulationResponse;
import com.example.backend.modules.admin.nomenclature.entity.Nomenclature;
import com.example.backend.modules.production.productionstock.entity.GlobalStock;
import com.example.backend.modules.admin.nomenclature.repository.NomenclatureRepository;
import com.example.backend.modules.production.productionstock.repository.GlobalStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private static final Logger logger = LoggerFactory.getLogger(SimulationService.class);

    private final GlobalStockRepository globalStockRepository;
    private final NomenclatureRepository nomenclatureRepository;

    public SimulationResponse checkFeasibility(SimulationRequest request) {
        logger.info("=== SIMULATION START ===");
        logger.info("Input - ProductId: {}, Reference: {}, Quantity: {}", 
                   request.getProductId(), request.getReference(), request.getQuantity());

        String reference = request.getReference();
        if (reference == null && request.getProductId() != null) {
            GlobalStock product = globalStockRepository.findById(request.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + request.getProductId()));
            reference = product.getRef();
            logger.info("Resolved productId {} to reference: {}", request.getProductId(), reference);
        }

        if (reference == null) {
            throw new RuntimeException("Reference or Product ID must be provided for simulation.");
        }

        Map<String, Double> requirements = new HashMap<>();
        try {
            calculateRequirements(reference, request.getQuantity(), requirements);
        } catch (RuntimeException e) {
            logger.error("Error calculating requirements: {}", e.getMessage());
            return SimulationResponse.builder()
                    .possible(false)
                    .message(e.getMessage())
                    .missingItems(new ArrayList<>())
                    .build();
        }

        logger.info("Calculated requirements: {}", requirements);

        List<SimulationResponse.MissingItem> missingItems = new ArrayList<>();
        boolean feasible = true;

        for (Map.Entry<String, Double> entry : requirements.entrySet()) {
            String ref = entry.getKey();
            Double required = entry.getValue();

            GlobalStock component = globalStockRepository.findByRef(ref).orElse(null);
            double available = (component != null) ? component.getQuantityAvailable() : 0.0;

            logger.info("Component: {} | Required: {} | Available: {} | Feasible: {}", 
                       ref, required, available, available >= required);

            if (available < required) {
                feasible = false;
                double missing = required - available;
                missingItems.add(SimulationResponse.MissingItem.builder()
                        .reference(ref)
                        .requiredQty(required)
                        .availableQty(available)
                        .missingQty(missing)
                        .build());
            }
        }

        logger.info("Overall feasible: {}", feasible);
        logger.info("Missing items: {}", missingItems);
        logger.info("=== SIMULATION END ===");

        return SimulationResponse.builder()
                .possible(feasible)
                .missingItems(missingItems)
                .message(feasible ? "All components are available in stock." 
                                  : "Insufficient stock for " + missingItems.size() + " components")
                .build();
    }

    private void calculateRequirements(String parentRef, Double quantity, Map<String, Double> requirements) {
        logger.info("calculateRequirements() - parentRef: {}, quantity: {}", parentRef, quantity);
        
        List<Nomenclature> components = nomenclatureRepository.findByParentRef(parentRef);
        logger.info("Found {} BOM entries for parentRef '{}'", components.size(), parentRef);
        
        for (Nomenclature n : components) {
            logger.info("  BOM: {} -> {} (qty: {})", parentRef, n.getComponentRef(), n.getQuantityRequired());
        }

        if (components.isEmpty()) {
            // Check if this product even exists in Nomenclature at all
            boolean existsAsComponent = nomenclatureRepository.existsByComponentRef(parentRef);
            logger.info("No BOM for {}. Is leaf node (raw material): {}", parentRef, existsAsComponent);
            
            if (!existsAsComponent) {
                logger.warn("Component {} not found as raw material either!", parentRef);
                throw new RuntimeException("Missing BOM definition for: " + parentRef);
            }
            // Leaf node (Raw material)
            Double accumulatedQty = requirements.getOrDefault(parentRef, 0.0) + quantity;
            requirements.put(parentRef, accumulatedQty);
            logger.info("Added leaf component {} with accumulated quantity: {}", parentRef, accumulatedQty);
            return;
        }

        for (Nomenclature n : components) {
            Double neededQty = quantity * n.getQuantityRequired();
            logger.info("Recursing into component {} with qty {} * {} = {}", 
                       n.getComponentRef(), quantity, n.getQuantityRequired(), neededQty);
            calculateRequirements(n.getComponentRef(), neededQty, requirements);
        }
    }
}
