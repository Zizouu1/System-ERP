package com.example.backend.modules.production.departement1.controller;

import com.example.backend.modules.admin.usermanagement.entity.User;
import com.example.backend.modules.production.departement1.dto.PsfIncomingRequest;
import com.example.backend.modules.production.departement1.dto.PsfOutgoingRequest;
import com.example.backend.modules.production.departement1.dto.PsfProductionRequest;
import com.example.backend.modules.production.departement1.service.PsfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/psf")
@RequiredArgsConstructor
public class PsfController {

    private final PsfService psfService;

    @PostMapping("/incoming")
    @PreAuthorize("hasAnyRole('PSF', 'ADMIN')")
    public ResponseEntity<String> registerIncoming(@RequestBody PsfIncomingRequest request,
            @AuthenticationPrincipal User user) {
        String qrCode = psfService.registerIncoming(request, user);
        return ResponseEntity.ok(qrCode);
    }

    @PostMapping("/outgoing")
    @PreAuthorize("hasAnyRole('PSF', 'ADMIN')")
    public ResponseEntity<Void> registerOutgoing(@RequestBody PsfOutgoingRequest request,
            @AuthenticationPrincipal User user) {
        psfService.registerOutgoing(request, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/production")
    @PreAuthorize("hasAnyRole('PSF', 'ADMIN')")
    public ResponseEntity<List<String>> declareProduction(@RequestBody PsfProductionRequest request,
            @AuthenticationPrincipal User user) {
        List<String> qrCodes = psfService.declareProduction(request, user);
        return ResponseEntity.ok(qrCodes);
    }

    @GetMapping("/stocks")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllStocks() {
        return ResponseEntity.ok(psfService.getAllStocks());
    }

    @GetMapping("/incoming")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllIncoming() {
        return ResponseEntity.ok(psfService.getAllIncoming());
    }

    @GetMapping("/outgoing")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllOutgoing() {
        return ResponseEntity.ok(psfService.getAllOutgoing());
    }
}
