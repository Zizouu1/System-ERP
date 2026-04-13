package com.example.backend.modules.production.departement1.logisticIncoming.controller;

import com.example.backend.modules.production.departement1.logisticIncoming.dto.IncomingMaterialDTO;
import com.example.backend.modules.production.departement1.logisticIncoming.dto.IncomingUpdateResponse;
import com.example.backend.modules.production.departement1.logisticIncoming.entity.IncomingMaterial;
import com.example.backend.modules.production.departement1.shared.entity.StockDep1;
import com.example.backend.modules.production.departement1.logisticIncoming.service.LogisticIncomingService;
import com.example.backend.modules.admin.usermanagement.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/psf/externe")
@RequiredArgsConstructor
public class LogisticIncomingController {

    private final LogisticIncomingService LogisticIncomingService;

    @PostMapping("/incoming")
    @PreAuthorize("hasRole('LOGISTIC')")
    public ResponseEntity<byte[]> registerIncoming(@RequestBody IncomingMaterialDTO incomingDTO,
            @AuthenticationPrincipal User user) {
        byte[] qrImage = LogisticIncomingService.registerIncoming(incomingDTO, user);
        if (qrImage == null || qrImage.length == 0) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qrImage);
    }

    @PutMapping("/incoming/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LOGISTIC')")
    public ResponseEntity<IncomingUpdateResponse> updateIncoming(@PathVariable Long id,
            @RequestBody IncomingMaterialDTO incomingDTO,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(LogisticIncomingService.updateIncoming(id, incomingDTO, user));
    }

    @DeleteMapping("/incoming/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteIncoming(@PathVariable Long id,
            @AuthenticationPrincipal User user) {
        LogisticIncomingService.deleteIncoming(id, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stocks")
    @PreAuthorize("hasAnyRole('LOGISTIC', 'ADMIN')")
    public ResponseEntity<List<StockDep1>> getAllStocks() {
        return ResponseEntity.ok(LogisticIncomingService.getAllStocks());
    }

    @PutMapping("/stocks/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StockDep1> updateStock(@PathVariable Long id, @RequestBody StockDep1 stockDTO) {
        return ResponseEntity.ok(LogisticIncomingService.updateStock(id, stockDTO));
    }

    @DeleteMapping("/stocks/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStock(@PathVariable Long id) {
        LogisticIncomingService.deleteStock(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/incoming")
    @PreAuthorize("hasAnyRole('ADMIN', 'LOGISTIC')")
    public ResponseEntity<List<IncomingMaterial>> getIncomingHistory(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(LogisticIncomingService.getIncomingHistory(user));
    }

    @GetMapping(value = "/qr/generate", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasRole('LOGISTIC')")
    public ResponseEntity<byte[]> generateQRCode(
            @RequestParam String reference,
            @RequestParam Double quantity,
            @RequestParam String lotNumber) {
        return ResponseEntity.ok(LogisticIncomingService.generateQRCode(reference, quantity, lotNumber));
    }
}
