package com.example.backend.modules.logistic.service;

import com.example.backend.modules.logistic.dto.IncomingMaterialDTO;
import com.example.backend.modules.logistic.dto.OutgoingMaterialDTO;
import com.example.backend.modules.logistic.entity.IncomingMaterial;
import com.example.backend.modules.logistic.entity.OutgoingMaterial;
import com.example.backend.modules.logistic.entity.ProductStock;
import com.example.backend.modules.logistic.qr.QRCodeGenerator;
import com.example.backend.modules.logistic.repository.IncomingMaterialRepository;
import com.example.backend.modules.logistic.repository.OutgoingMaterialRepository;
import com.example.backend.modules.logistic.repository.ProductStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LogisticService {

    private final IncomingMaterialRepository incomingRepository;
    private final OutgoingMaterialRepository outgoingRepository;
    private final ProductStockRepository stockRepository;
    private final QRCodeGenerator qrCodeGenerator;

    @Transactional
    public IncomingMaterial processIncoming(IncomingMaterialDTO incomingDTO) {
        IncomingMaterial incoming = IncomingMaterial.builder()
                .reference(incomingDTO.getReference())
                .quantity(incomingDTO.getQuantity())
                .lotNumber(incomingDTO.getLotNumber())
                .notes(incomingDTO.getNotes())
                .operationDate(
                        incomingDTO.getOperationDate() != null ? incomingDTO.getOperationDate() : LocalDateTime.now())
                .build();

        IncomingMaterial savedIncoming = incomingRepository.save(incoming);

        // Update or create stock
        ProductStock stock = stockRepository.findByReference(incoming.getReference())
                .orElse(ProductStock.builder()
                        .reference(incoming.getReference())
                        .totalQuantity(0.0)
                        .storeQuantity(0.0)
                        .consumedQuantity(0.0)
                        .build());

        stock.setTotalQuantity(stock.getTotalQuantity() + incoming.getQuantity());
        stock.setStoreQuantity(stock.getStoreQuantity() + incoming.getQuantity());

        stockRepository.save(stock);
        return savedIncoming;
    }

    @Transactional
    public OutgoingMaterial processOutgoing(OutgoingMaterialDTO outgoingDTO) {
        // Check stock availability
        ProductStock stock = stockRepository.findByReference(outgoingDTO.getReference())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product reference not found: " + outgoingDTO.getReference()));

        if (stock.getStoreQuantity() < outgoingDTO.getQuantityOut()) {
            throw new IllegalArgumentException("Insufficient stock for reference: " + outgoingDTO.getReference());
        }

        OutgoingMaterial outgoing = OutgoingMaterial.builder()
                .reference(outgoingDTO.getReference())
                .lotNumber(outgoingDTO.getLotNumber())
                .quantityOut(outgoingDTO.getQuantityOut())
                .notes(outgoingDTO.getNotes())
                .operationDate(
                        outgoingDTO.getOperationDate() != null ? outgoingDTO.getOperationDate() : LocalDateTime.now())
                .build();

        OutgoingMaterial savedOutgoing = outgoingRepository.save(outgoing);

        // Update stock
        stock.setStoreQuantity(stock.getStoreQuantity() - outgoing.getQuantityOut());
        stock.setConsumedQuantity(stock.getTotalQuantity() - stock.getStoreQuantity());

        stockRepository.save(stock);
        return savedOutgoing;
    }

    public List<IncomingMaterial> getAllIncoming() {
        return incomingRepository.findAll();
    }

    public List<OutgoingMaterial> getAllOutgoing() {
        return outgoingRepository.findAll();
    }

    public List<ProductStock> getAllStock() {
        return stockRepository.findAll();
    }

    public byte[] generateQRCode(String reference, Double quantity, String lotNumber) {
        try {
            String data = qrCodeGenerator.generateDataString(reference, quantity, lotNumber);
            return qrCodeGenerator.generateQRCodeImage(data, 200, 200);
        } catch (Exception e) {
            throw new RuntimeException("Error generating QR code", e);
        }
    }
}
