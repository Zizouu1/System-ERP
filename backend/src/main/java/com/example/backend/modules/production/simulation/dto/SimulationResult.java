package com.example.backend.modules.production.simulation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SimulationResult {

    private boolean possible;
    private List<MissingItem> missingItems;

}
