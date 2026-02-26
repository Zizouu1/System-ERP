package com.example.backend.modules.production.departement1.productsInterne.service;

import com.example.backend.modules.admin.usermanagement.entity.User;
import com.example.backend.modules.production.departement1.productsInterne.dto.PsfProductionRequest;
import com.example.backend.modules.production.departement1.productsInterne.entity.*;
import com.example.backend.modules.production.departement1.productsInterne.repository.*;
import com.example.backend.modules.production.departement1.shared.entity.StockDep1;
import com.example.backend.modules.production.departement1.shared.entity.StockSource;
import com.example.backend.modules.production.departement1.shared.service.StockDep1Service;
import com.example.backend.modules.production.productionstock.entity.ProductType;
import com.example.backend.modules.production.productionstock.service.ProductService;
import com.example.backend.modules.production.productionstock.util.QRCodeGenerator;
import com.example.backend.modules.production.productionstock.util.ProductionTimeCalculator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductInterneService {

    private final PsfProductionRepository productionRepository;
    private final ProductService productService;
    private final StockDep1Service stockDep1Service;
    private final QRCodeGenerator qrCodeGenerator;

    @Transactional
    public List<byte[]> declareProduction(PsfProductionRequest request, User user) {
        if (request.getQuantityPerBatch() <= 0) {
            throw new IllegalArgumentException("Quantity per batch must be greater than zero.");
        }
        if (request.getQuantity() < 0) {
            throw new IllegalArgumentException("Total produced quantity cannot be negative.");
        }

        int batches = (int) (request.getQuantity() / request.getQuantityPerBatch());
        if (batches <= 0) {
            throw new RuntimeException("Total produced quantity must be greater than batch quantity.");
        }

        String productionId = UUID.randomUUID().toString();
        String internalLotNumber = productionId;

        LocalTime startTimeParsed = (request.getStartTime() != null) ? LocalTime.parse(request.getStartTime()) : null;
        LocalTime endTimeParsed = (request.getEndTime() != null) ? LocalTime.parse(request.getEndTime()) : null;

        PsfProduction production = PsfProduction.builder()
                .reference(request.getReference())
                .quantity(request.getQuantity())
                .quantityPerBatch(request.getQuantityPerBatch())
                .batches(batches)
                .timestamp(LocalDateTime.now())
                .operatorMatricule(request.getOperatorMatricule())
                .scrapQuantity(request.getScrapQuantity())
                .startTime(startTimeParsed)
                .endTime(endTimeParsed)
                .user(user)
                .build();

        if (startTimeParsed != null && endTimeParsed != null) {
            ProductionTimeCalculator.ProductionMetrics metrics = ProductionTimeCalculator.calculate(
                    startTimeParsed,
                    endTimeParsed,
                    request.getQuantity());

            production.setRawTime(metrics.rawTime());
            production.setEffectiveTime(metrics.effectiveTime());
            production.setPerformance(metrics.performance());
        }

        productionRepository.save(production);

        double netQuantity = request.getQuantity() - request.getScrapQuantity();
        if (netQuantity < 0) {
            throw new IllegalArgumentException("Scrap quantity cannot exceed total produced quantity.");
        }

        stockDep1Service.addStock(request.getReference(), internalLotNumber, netQuantity, StockSource.PSF);

        productService.declareProduction(
                request.getReference(),
                netQuantity,
                "PSF-" + productionId);

        List<byte[]> qrCodes = new ArrayList<>();
        int fullBatches = (int) (request.getQuantity() / request.getQuantityPerBatch());
        int remainder = (int) (request.getQuantity() % request.getQuantityPerBatch());
        int lastBatchQuantity = remainder - request.getScrapQuantity();
        if (lastBatchQuantity < 0)
            lastBatchQuantity = 0;

        for (int i = 0; i < fullBatches; i++) {
            qrCodes.add(qrCodeGenerator.generateQRCodeImage(
                    qrCodeGenerator.joinFields(request.getReference(), request.getQuantityPerBatch(), productionId,
                            (i + 1)),
                    200, 200));
        }
        if (lastBatchQuantity > 0) {
            qrCodes.add(qrCodeGenerator.generateQRCodeImage(
                    qrCodeGenerator.joinFields(request.getReference(), lastBatchQuantity, productionId,
                            (fullBatches + 1)),
                    200, 200));
        }

        return qrCodes;
    }

    public List<PsfProduction> getAllProductions() {
        return productionRepository.findAll();
    }

    public List<StockDep1> getAllStocks() {
        return stockDep1Service.getAllStockBySource(StockSource.PSF);
    }

    @Transactional
    public PsfProduction updateProduction(Long id, PsfProductionRequest request) {
        PsfProduction existing = productionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Production not found"));

        double oldNetQty = existing.getQuantity() - existing.getScrapQuantity();
        String oldInternalLot = existing.getProductionId();

        stockDep1Service.deductStock(existing.getReference(), oldInternalLot, oldNetQty, StockSource.PSF);

        productService.revertProduction(existing.getReference(), oldNetQty, "PSF-" + existing.getProductionId());

        if (request.getQuantity() < request.getScrapQuantity()) {
            throw new IllegalArgumentException("Scrap cannot exceed total quantity");
        }
        double newNetQty = request.getQuantity() - request.getScrapQuantity();

        existing.setReference(request.getReference());
        existing.setQuantity(request.getQuantity());
        existing.setScrapQuantity(request.getScrapQuantity());
        existing.setQuantityPerBatch(request.getQuantityPerBatch());
        existing.setBatches((int) (request.getQuantity() / request.getQuantityPerBatch()));
        existing.setOperatorMatricule(request.getOperatorMatricule());
        LocalTime start = request.getStartTime() != null ? LocalTime.parse(request.getStartTime()) : null;
        LocalTime end = request.getEndTime() != null ? LocalTime.parse(request.getEndTime()) : null;
        existing.setStartTime(start);
        existing.setEndTime(end);

        if (start != null && end != null) {
            ProductionTimeCalculator.ProductionMetrics metrics = ProductionTimeCalculator.calculate(start, end,
                    request.getQuantity());
            existing.setRawTime(metrics.rawTime());
            existing.setEffectiveTime(metrics.effectiveTime());
            existing.setPerformance(metrics.performance());
        }

        stockDep1Service.addStock(request.getReference(), oldInternalLot, request.getProductType(), newNetQty,
                StockSource.PSF);

        productService.declareProduction(request.getReference(), newNetQty, "PSF-" + existing.getProductionId());

        return productionRepository.save(existing);
    }

    @Transactional
    public void deleteProduction(Long id) {
        PsfProduction existing = productionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Production not found"));

        double oldNetQty = existing.getQuantity() - existing.getScrapQuantity();
        String oldInternalLot = existing.getProductionId();

        stockDep1Service.deductStock(existing.getReference(), oldInternalLot, oldNetQty, StockSource.PSF);

        productService.revertProduction(existing.getReference(), oldNetQty, "PSF-" + existing.getProductionId());
        productionRepository.delete(existing);
    }

    @Transactional
    public StockDep1 updateStock(Long id, StockDep1 request) {
        return stockDep1Service.updateStock(id, request);
    }

    @Transactional
    public void deleteStock(Long id) {
        stockDep1Service.deleteStock(id);
    }
}
