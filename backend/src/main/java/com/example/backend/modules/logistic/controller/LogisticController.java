package com.example.backend.modules.logistic.controller;

import com.example.backend.modules.logistic.dto.IncomingMaterialDTO;
import com.example.backend.modules.logistic.dto.OutgoingMaterialDTO;
import com.example.backend.modules.logistic.entity.IncomingMaterial;
import com.example.backend.modules.logistic.entity.OutgoingMaterial;
import com.example.backend.modules.logistic.entity.ProductStock;
import com.example.backend.modules.logistic.service.LogisticService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logistic")
@RequiredArgsConstructor
public class LogisticController {

    private final LogisticService logisticService;

    @PostMapping("/incoming")
    public ResponseEntity<IncomingMaterial> addIncoming(@RequestBody IncomingMaterialDTO incomingDTO) {
        return ResponseEntity.ok(logisticService.processIncoming(incomingDTO));
    }

    @PostMapping("/outgoing")
    public ResponseEntity<OutgoingMaterial> addOutgoing(@RequestBody OutgoingMaterialDTO outgoingDTO) {
        return ResponseEntity.ok(logisticService.processOutgoing(outgoingDTO));
    }

    @GetMapping("/stock")
    public ResponseEntity<List<ProductStock>> getAllStock() {
        return ResponseEntity.ok(logisticService.getAllStock());
    }

    @GetMapping("/incoming")
    public ResponseEntity<List<IncomingMaterial>> getIncomingHistory() {
        return ResponseEntity.ok(logisticService.getAllIncoming());
    }

    @GetMapping("/outgoing")
    public ResponseEntity<List<OutgoingMaterial>> getOutgoingHistory() {
        return ResponseEntity.ok(logisticService.getAllOutgoing());
    }

    @GetMapping(value = "/qr/generate", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateQRCode(
            @RequestParam String reference,
            @RequestParam Double quantity,
            @RequestParam String lotNumber) {
        return ResponseEntity.ok(logisticService.generateQRCode(reference, quantity, lotNumber));
    }
}
