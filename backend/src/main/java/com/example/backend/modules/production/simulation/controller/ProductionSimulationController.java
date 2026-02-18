package com.example.backend.modules.production.simulation.controller;

import com.example.backend.modules.production.simulation.dto.*;
import com.example.backend.modules.production.simulation.ProductionSimulationService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/simulation")
@RequiredArgsConstructor
public class ProductionSimulationController {

    private final ProductionSimulationService simulationService;

    @PostMapping
    public SimulationResult simulate(@RequestBody List<SimulationRequest> requests) {
        return simulationService.simulate(requests);
    }
}
