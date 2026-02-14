package com.example.backend.modules.production.departement2.controller;

import com.example.backend.modules.production.departement2.dto.PfProductionRequest;
import com.example.backend.modules.production.departement2.entity.PfProduction;
import com.example.backend.modules.production.departement2.service.PfProductionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/production/dept2")
@RequiredArgsConstructor
public class PfProductionController {

    private final PfProductionService pfProductionService;

    @PostMapping
    public ResponseEntity<PfProduction> createProduction(@RequestBody PfProductionRequest request) {
        try {
            PfProduction savedProduction = pfProductionService.saveProduction(request);
            return ResponseEntity.ok(savedProduction);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<PfProduction>> getAllProductions() {
        return ResponseEntity.ok(pfProductionService.getAllProductions());
    }
}
