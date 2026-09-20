package com.example.backend.modules.admin.productiondelay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelMetricsResponse {
    private Double accuracy;
    private Double precision;
    private Double recall;
    private Double f1;
    @JsonProperty("roc_auc")
    private Double rocAuc;
}