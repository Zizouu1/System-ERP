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

    private final GlobalStockRepository globalStockRepository;
    private final NomenclatureRepository nomenclatureRepository;

    public SimulationResult simulate(List<SimulationRequest> requests) {

        Map<String, Double> totalRequired = new HashMap<>();

        try {
            // 🔁 Explode all selected products
            for (SimulationRequest request : requests) {
                String reference = request.getReference();
                if (reference == null && request.getProductId() != null) {
                    GlobalStock product = globalStockRepository.findById(request.getProductId())
                            .orElseThrow(() -> new RuntimeException("Produit introuvable"));
                    reference = product.getRef();
                }

                if (reference == null) {
                    throw new RuntimeException("Une référence doit être fournie pour la simulation");
                }

                explode(reference, request.getQuantity(), totalRequired);
            }
        } catch (RuntimeException e) {
            // If any BOM is missing, we can return a failure immediately or gather all
            // errors.
            // For now, return immediate failure message.
            return new SimulationResult(false, Collections.emptyList()); // Simplified for now
        }

        // 🔎 Compare with stock
        List<MissingItem> missingItems = new ArrayList<>();

        for (Map.Entry<String, Double> entry : totalRequired.entrySet()) {

            String ref = entry.getKey();
            Double requiredQty = entry.getValue();

            GlobalStock stockItem = globalStockRepository.findByRef(ref).orElse(null);
            Double availableQty = (stockItem != null) ? stockItem.getQuantityAvailable() : 0.0;

            if (requiredQty > availableQty) {

                Double missingQty = requiredQty - availableQty;

                missingItems.add(
                        new MissingItem(
                                ref,
                                requiredQty,
                                availableQty,
                                missingQty));
            }
        }

        boolean possible = missingItems.isEmpty();

        return new SimulationResult(possible, missingItems);
    }

    // 🔥 Recursive BOM Explosion
    private void explode(String parentRef,
            Double quantity,
            Map<String, Double> totalRequired) {

        List<Nomenclature> components = nomenclatureRepository.findByParentRef(parentRef);

        // If no children → check if it's a known product or missing BOM
        if (components.isEmpty()) {
            boolean existsInBom = nomenclatureRepository.existsByParentRef(parentRef) ||
                    nomenclatureRepository.existsByComponentRef(parentRef);
            if (!existsInBom) {
                throw new RuntimeException("Missing BOM definition for: " + parentRef);
            }
            totalRequired.merge(parentRef, quantity, Double::sum);
            return;
        }

        // If has children → explode each component
        for (Nomenclature bom : components) {
            String componentRef = bom.getComponentRef();
            Double requiredQty = quantity * bom.getQuantityRequired();

            explode(componentRef, requiredQty, totalRequired);
        }
    }
}
