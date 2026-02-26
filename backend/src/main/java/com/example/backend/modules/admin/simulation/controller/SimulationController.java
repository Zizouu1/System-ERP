package com.example.backend.modules.admin.simulation.controller;

import com.example.backend.modules.admin.simulation.dto.SimulationRequest;
import com.example.backend.modules.admin.simulation.dto.SimulationResponse;
import com.example.backend.modules.admin.simulation.service.SimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
@CrossOrigin
@PreAuthorize("hasRole('ADMIN')")
public class SimulationController {

    private final SimulationService simulationService;

    @PostMapping("/check")
    public SimulationResponse check(@RequestBody SimulationRequest request) {
        return simulationService.checkFeasibility(request);
    }
}
