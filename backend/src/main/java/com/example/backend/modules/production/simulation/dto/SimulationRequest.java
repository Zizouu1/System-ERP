package com.example.backend.modules.production.simulation.dto;

import lombok.Data;

@Data
public class SimulationRequest {

    private Long productId;
    private Double quantity;

}
