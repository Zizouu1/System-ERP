package com.example.backend.modules.admin.simulation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResponse {
    private boolean possible;  // Changed from 'feasible' to match frontend
    private List<MissingItem> missingItems;  // Structured list instead of plain message
    private List<RequiredItem> requiredItems;
    private String message;  // Keep for backward compatibility

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissingItem {
        private String reference;
        private Double requiredQty;
        private Double availableQty;
        private Double missingQty;  // requiredQty - availableQty
        private List<ProductContribution> productContributions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductContribution {
        private String productRef;
        private String productDesignation;
        private Double producedQty;
        private Double requiredQty;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequiredItem {
        private String reference;
        private Double requiredQty;
        private Double availableQty;
        private Double missingQty;
    }
}
