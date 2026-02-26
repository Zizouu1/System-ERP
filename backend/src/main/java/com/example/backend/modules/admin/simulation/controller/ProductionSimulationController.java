package com.example.backend.modules.admin.simulation.controller;

import com.example.backend.modules.admin.simulation.dto.*;
import com.example.backend.modules.admin.simulation.service.ProductionSimulationService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/simulation")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ProductionSimulationController {

    private final ProductionSimulationService simulationService;

    @PostMapping
    public SimulationResult simulate(@RequestBody List<SimulationRequest> requests) {
        return simulationService.simulate(requests);
    }
}
