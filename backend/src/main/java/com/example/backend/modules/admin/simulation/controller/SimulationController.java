package com.example.backend.modules.admin.simulation.controller;

import com.example.backend.modules.admin.simulation.dto.SimulationBatchRequest;
import com.example.backend.modules.admin.simulation.dto.SimulationRequest;
import com.example.backend.modules.admin.simulation.dto.SimulationResponse;
import com.example.backend.modules.admin.simulation.service.SimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SimulationController {

    private final SimulationService simulationService;

    @PostMapping("/check")
    public ResponseEntity<?> check(@RequestBody SimulationRequest request) {
        try {
            return ResponseEntity.ok(simulationService.checkFeasibility(request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/check-bulk")
    public ResponseEntity<?> checkBulk(@RequestBody SimulationBatchRequest request) {
        try {
            return ResponseEntity.ok(simulationService.checkFeasibilityBatch(request.getItems()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}
