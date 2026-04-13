package com.example.backend.modules.production.departement1.shared.controller;

import com.example.backend.modules.admin.usermanagement.entity.User;
import com.example.backend.modules.production.departement1.shared.dto.OutgoingDTO;
import com.example.backend.modules.production.departement1.shared.service.OutgoingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/outgoing")
@RequiredArgsConstructor
public class OutgoingController {

    private final OutgoingService outgoingService;

    @PostMapping
    @PreAuthorize("hasAnyRole('PSF', 'LOGISTIC')")
    public ResponseEntity<Void> registerOutgoing(@RequestBody OutgoingDTO request,
            @AuthenticationPrincipal User user) {
        outgoingService.registerOutgoing(request, user);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PSF', 'LOGISTIC')")
    public ResponseEntity<?> updateOutgoing(@PathVariable Long id,
            @RequestBody OutgoingDTO request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(outgoingService.updateOutgoing(id, request, user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOutgoing(@PathVariable Long id,
            @AuthenticationPrincipal User user) {
        outgoingService.deleteOutgoing(id, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LOGISTIC', 'PSF')")
    public ResponseEntity<?> getAllOutgoing(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(outgoingService.getOutgoingHistory(user));
    }
}
