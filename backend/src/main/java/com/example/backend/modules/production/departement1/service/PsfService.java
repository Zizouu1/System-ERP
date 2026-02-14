package com.example.backend.modules.production.departement1.service;

import com.example.backend.modules.admin.usermanagement.entity.User;
import com.example.backend.modules.production.departement1.dto.PsfIncomingRequest;
import com.example.backend.modules.production.departement1.dto.PsfOutgoingRequest;
import com.example.backend.modules.production.departement1.dto.PsfProductionRequest;
import com.example.backend.modules.production.departement1.entity.*;
import com.example.backend.modules.production.departement1.repository.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.backend.modules.production.shared.util.ProductionTimeCalculator;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PsfService {

    private final PsfStockRepository stockRepository;
    private final PsfIncomingRepository incomingRepository;
    private final PsfOutgoingRepository outgoingRepository;
    private final PsfProductionRepository productionRepository;

    @Transactional
    public String registerIncoming(PsfIncomingRequest request, User user) {
        if (request.getQuantity() < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }

        // Generate standardized lotNumber if not provided
        String lotNumber = (request.getLotNumber() != null && !request.getLotNumber().isEmpty())
                ? request.getLotNumber()
                : "EXT-" + UUID.randomUUID().toString();

        // Update or Create Stock
        PsfStock stock = stockRepository.findByReferenceAndLotNumber(request.getReference(), lotNumber)
                .orElse(PsfStock.builder()
                        .reference(request.getReference())
                        .lotNumber(lotNumber)
                        .productType(request.getProductType())
                        .isInternal(false)
                        .producedByCutMachine(false)
                        .totalQuantity(0)
                        .storeQuantity(0)
                        .build());

        stock.setTotalQuantity(stock.getTotalQuantity() + request.getQuantity());
        stock.setStoreQuantity(stock.getStoreQuantity() + request.getQuantity());
        stockRepository.save(stock);

        // Persist Incoming Operation
        PsfIncoming incoming = PsfIncoming.builder()
                .reference(request.getReference())
                .quantity(request.getQuantity())
                .lotNumber(lotNumber)
                .productType(request.getProductType())
                .timestamp(LocalDateTime.now())
                .user(user)
                .build();
        incomingRepository.save(incoming);

        // Generate QR Code
        // Format: référence$quantité$lot_number
        String qrContent = request.getReference() + "$" + request.getQuantity() + "$" + lotNumber;
        return generateQrCodeImage(qrContent);
    }

    @Transactional
    public void registerOutgoing(PsfOutgoingRequest request, User user) {
        if (request.getQuantityOut() < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }

        PsfStock stock = stockRepository.findByReferenceAndLotNumber(request.getReference(), request.getLotNumber())
                .orElseThrow(() -> new RuntimeException("Stock not found for reference: " + request.getReference()
                        + " and lot: " + request.getLotNumber()));

        if (stock.getStoreQuantity() < request.getQuantityOut()) {
            throw new RuntimeException("Insufficient stock quantity. Available: " + stock.getStoreQuantity());
        }

        stock.setStoreQuantity(stock.getStoreQuantity() - request.getQuantityOut());
        stockRepository.save(stock);

        // Determine isInternal: use request value if present, otherwise fallback to
        // stock value
        boolean isInternal = (request.getIsInternal() != null) ? request.getIsInternal() : stock.isInternal();

        PsfOutgoing outgoing = PsfOutgoing.builder()
                .reference(request.getReference())
                .lotNumber(request.getLotNumber())
                .quantityOut(request.getQuantityOut())
                .notes(request.getNotes())
                .operationDate(request.getOperationDate() != null ? request.getOperationDate() : LocalDateTime.now())
                .user(user)
                .isInternal(isInternal)
                .build();
        outgoingRepository.save(outgoing);
    }

    @Transactional
    public List<String> declareProduction(PsfProductionRequest request, User user) {
        if (request.getQuantityPerBatch() <= 0) {
            throw new IllegalArgumentException("Quantity per batch must be greater than zero.");
        }
        if (request.getTotalProducedQuantity() < 0) {
            throw new IllegalArgumentException("Total produced quantity cannot be negative.");
        }

        int batches = (int) (request.getTotalProducedQuantity() / request.getQuantityPerBatch());
        if (batches <= 0) {
            throw new RuntimeException("Total produced quantity must be greater than batch quantity.");
        }

        // Validate Time - Handled in calculate method later, but checking nulls here
        // logic is acceptable to keep for early fail or structure.
        // We can remove the explicit call to validateTime if we are calling calculate
        // later, but logic flow shows we check nulls.
        // I will keep the null check but remove the redundant validateTime call if I'm
        // sure it's called later.
        // Actually, let's just keep strict validation here for the request object
        // before processing other things.
        if (request.getStartTime() != null && request.getEndTime() != null) {
            // Validation happens in calculate method
        }

        String productionId = UUID.randomUUID().toString();
        // Use a standardized format for internal lot numbers to distinguish from
        // incoming
        // Using productionId as part of lotNumber to ensure uniqueness but adding
        // prefix
        // NOTE: If front-end scans QR code containing raw productionId, it must handle
        // mapping
        // However, based on requirements, we will use productionId as the lot reference
        // for simplicity
        // as the unique constraint is on (reference, lotNumber).
        String internalLotNumber = productionId;

        PsfProduction production = PsfProduction.builder()
                .reference(request.getReference())
                .totalProducedQuantity(request.getTotalProducedQuantity())
                .quantityPerBatch(request.getQuantityPerBatch())
                .batches(batches)
                .productionId(productionId)
                .timestamp(LocalDateTime.now())
                .user(user)
                .operatorMatricule(request.getOperatorMatricule())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        if (request.getStartTime() != null && request.getEndTime() != null) {
            ProductionTimeCalculator.ProductionMetrics metrics = ProductionTimeCalculator.calculate(
                    request.getStartTime(),
                    request.getEndTime(),
                    request.getTotalProducedQuantity());

            production.setRawTime(metrics.rawTime());
            production.setEffectiveTime(metrics.effectiveTime());
            production.setPerformance(metrics.performance());
        }

        productionRepository.save(production);

        // Update Stock (Internal Production)
        PsfStock stock = stockRepository.findByReferenceAndLotNumber(request.getReference(), internalLotNumber)
                .orElse(PsfStock.builder()
                        .reference(request.getReference())
                        .lotNumber(internalLotNumber)
                        .productType(request.getProductType())
                        .isInternal(true)
                        .producedByCutMachine(request.isProducedByCutMachine())
                        .totalQuantity(0)
                        .storeQuantity(0)
                        .build());

        stock.setTotalQuantity(stock.getTotalQuantity() + request.getTotalProducedQuantity());
        stock.setStoreQuantity(stock.getStoreQuantity() + request.getTotalProducedQuantity());
        stockRepository.save(stock);

        List<String> qrCodes = new ArrayList<>();
        for (int i = 0; i < batches; i++) {
            // QR format: référence$quantitéParLot$productionId$i
            String qrContent = request.getReference() + "$" + request.getQuantityPerBatch() + "$" + productionId + "$"
                    + (i + 1);
            qrCodes.add(generateQrCodeImage(qrContent));
        }
        return qrCodes;
    }

    public List<PsfStock> getAllStocks() {
        return stockRepository.findAll();
    }

    public List<PsfIncoming> getAllIncoming() {
        return incomingRepository.findAll();
    }

    public List<PsfOutgoing> getAllOutgoing() {
        return outgoingRepository.findAll();
    }

    private String generateQrCodeImage(String content) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 200, 200);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();
            return Base64.getEncoder().encodeToString(pngData);
        } catch (Exception e) {
            throw new RuntimeException("Error generating QR code", e);
        }
    }
}
