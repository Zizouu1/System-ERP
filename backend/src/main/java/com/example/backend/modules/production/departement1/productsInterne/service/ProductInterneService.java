package com.example.backend.modules.production.departement1.productsInterne.service;

import com.example.backend.modules.admin.product.entity.ProductTypeEnum;
import com.example.backend.modules.production.departement1.productsInterne.dto.PsfProductionRequest;
import com.example.backend.modules.production.departement1.productsInterne.entity.PsfProduction;
import com.example.backend.modules.production.departement1.productsInterne.repository.PsfProductionRepository;
import com.example.backend.modules.production.departement1.shared.entity.StockDep1;
import com.example.backend.modules.production.departement1.shared.entity.StockSource;
import com.example.backend.modules.production.departement1.shared.repository.StockDep1Repository;
import com.example.backend.modules.production.departement1.shared.service.StockDep1Service;
import com.example.backend.modules.production.productionstock.service.ProductService;
import com.example.backend.modules.production.productionstock.util.QRCodeGenerator;

import io.micrometer.common.lang.NonNull;

import com.example.backend.modules.production.productionstock.util.ProductionTimeCalculator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductInterneService {

    private final PsfProductionRepository productionRepository;
    private final ProductService globalStockProductService;
    private final StockDep1Service stockDep1Service;
    private final StockDep1Repository stockDep1Repository;
    private final com.example.backend.modules.admin.product.service.ProductService masterProductService;
    private final QRCodeGenerator qrCodeGenerator;

    /**
     * Validates that a reference exists in the Product master table.
     * Ensures the product type is SEMI_FINI for PSF production.
     * Throws exception if ref not found or product type is incorrect.
     */
    private void validateProductReference(String ref) {
        masterProductService.validateProductReference(ref);
        var product = masterProductService.getProductByRef(ref);
        // Validate that the product type is SEMI_FINI for PSF production
        if (product.getProductType() != ProductTypeEnum.SEMI_FINI) {
            throw new IllegalArgumentException(
                    "Le produit '" + ref + "' n'est pas autorisé pour cette opération. " +
                    "Seuls les semi-finis peuvent être produits par le département PSF.");
        }
    }

    @Transactional
    public List<byte[]> declareProduction(PsfProductionRequest request) {
        // Validate product reference exists in Product master
        validateProductReference(request.getReference());

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

        LocalTime startTimeParsed = parseTime(request.getStartTime());
        LocalTime endTimeParsed = parseTime(request.getEndTime());

        PsfProduction production = PsfProduction.builder()
                .reference(request.getReference())
                .quantity(request.getQuantity())
                .quantityPerBatch(request.getQuantityPerBatch())
                .batches(batches)
                .timestamp(LocalDateTime.now())
                .operatorMatricule(request.getOperatorMatricule())
                .scrapQuantity(request.getScrapQuantity())
                .producedByCutMachine(request.isProducedByCutMachine())
                .startTime(startTimeParsed)
                .endTime(endTimeParsed)
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
        PsfProduction savedProduction = productionRepository.save(production);
        String lotNumber = String.valueOf(savedProduction.getId());

        double netQuantity = request.getQuantity() - request.getScrapQuantity();
        if (netQuantity < 0) {
            throw new IllegalArgumentException("Scrap quantity cannot exceed total produced quantity.");
        }

        stockDep1Service.addStock(request.getReference(), lotNumber, ProductTypeEnum.SEMI_FINI, netQuantity, StockSource.PSF);
        // We also need to add individual batch stocks for traceability if needed,
        // but the scan mapping expects exact match for P-id.batch.
        // Let's replace the single addStock above with per-batch addStock.
        /* 
        stockDep1Service.addStock(request.getReference(), lotNumber, ProductTypeEnum.SEMI_FINI, netQuantity, StockSource.PSF);
        */
        
        // Remove the previous single addStock call and use the loop below instead
        // Wait, I'll just remove the single call and implement the loop.
        
        int fullBatches = (int) (netQuantity / request.getQuantityPerBatch());
        double remainder = netQuantity % request.getQuantityPerBatch();
        List<byte[]> qrCodes = new ArrayList<>();

        for (int i = 0; i < fullBatches; i++) {
            String batchLot = "P-" + savedProduction.getId() + "." + (i + 1);
            stockDep1Service.addStock(request.getReference(), batchLot, ProductTypeEnum.SEMI_FINI, request.getQuantityPerBatch(), StockSource.PSF);
            qrCodes.add(qrCodeGenerator.generateQRCodeImage(
                    qrCodeGenerator.joinFields(request.getReference(), request.getQuantityPerBatch(), batchLot),
                    200, 200));
        }
        if (remainder > 0) {
            String batchLot = "P-" + savedProduction.getId() + "." + (fullBatches + 1);
            stockDep1Service.addStock(request.getReference(), batchLot, ProductTypeEnum.SEMI_FINI, remainder, StockSource.PSF);
            qrCodes.add(qrCodeGenerator.generateQRCodeImage(
                    qrCodeGenerator.joinFields(request.getReference(), remainder, batchLot),
                    200, 200));
        }

        globalStockProductService.declareProduction(
                request.getReference(),
                netQuantity,
                "PSF-" + productionId,
                ProductTypeEnum.SEMI_FINI);

        return qrCodes;
    }

    public List<PsfProduction> getAllProductions() {
        return productionRepository.findAll();
    }

    public List<StockDep1> getAllStocks() {
        return stockDep1Service.getAllStockBySource(null);
    }

    @Transactional
    public PsfProduction updateProduction(Long id, PsfProductionRequest request) {
        PsfProduction existing = productionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Production non trouvée"));

        // Validate the new reference if it has changed
        if (!existing.getReference().equals(request.getReference())) {
            validateProductReference(request.getReference());
        }

        double oldNetQty = existing.getQuantity() - existing.getScrapQuantity();
        
        // Find and remove all batch stock entries for this production
        String lotPrefix = "P-" + existing.getId() + ".";
        List<StockDep1> batchStocks = stockDep1Repository.findByReferenceAndSourceAndLotNumberStartingWith(
                existing.getReference(), StockSource.PSF, lotPrefix);
        
        for (StockDep1 s : batchStocks) {
            stockDep1Service.deductStock(s.getReference(), s.getLotNumber(), s.getStoreQuantity(), StockSource.PSF);
        }

        globalStockProductService.revertProduction(existing.getReference(), oldNetQty, "PSF-" + existing.getId());

        if (request.getQuantity() < request.getScrapQuantity()) {
            throw new IllegalArgumentException("La quantité de rebut ne peut pas dépasser la quantité totale");
        }
        double newNetQty = request.getQuantity() - request.getScrapQuantity();

        existing.setReference(request.getReference());
        existing.setQuantity(request.getQuantity());
        existing.setScrapQuantity(request.getScrapQuantity());
        existing.setQuantityPerBatch(request.getQuantityPerBatch());
        existing.setBatches((int) (request.getQuantity() / request.getQuantityPerBatch()));
        existing.setOperatorMatricule(request.getOperatorMatricule());
        existing.setProducedByCutMachine(request.isProducedByCutMachine());
        LocalTime start = parseTime(request.getStartTime());
        LocalTime end = parseTime(request.getEndTime());
        existing.setStartTime(start);
        existing.setEndTime(end);

        if (start != null && end != null) {
            ProductionTimeCalculator.ProductionMetrics metrics = ProductionTimeCalculator.calculate(start, end,
                    request.getQuantity());
            existing.setRawTime(metrics.rawTime());
            existing.setEffectiveTime(metrics.effectiveTime());
            existing.setPerformance(metrics.performance());
        }

        // Re-add stock as batches
        int fullBatches = (int) (newNetQty / request.getQuantityPerBatch());
        double remainder = newNetQty % request.getQuantityPerBatch();
        for (int i = 0; i < fullBatches; i++) {
            stockDep1Service.addStock(request.getReference(), lotPrefix + (i + 1), ProductTypeEnum.SEMI_FINI, request.getQuantityPerBatch(), StockSource.PSF);
        }
        if (remainder > 0) {
            stockDep1Service.addStock(request.getReference(), lotPrefix + (fullBatches + 1), ProductTypeEnum.SEMI_FINI, remainder, StockSource.PSF);
        }

        globalStockProductService.declareProduction(request.getReference(), newNetQty, "PSF-" + existing.getId(), ProductTypeEnum.SEMI_FINI);
        return productionRepository.save(existing);
    }

    @Transactional
    public void deleteProduction(@NonNull Long id) {
        PsfProduction existing = productionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Production introuvable"));

        double oldNetQty = existing.getQuantity() - existing.getScrapQuantity();
        
        // Find and remove all batch stock entries for this production
        String lotPrefix = "P-" + existing.getId() + ".";
        List<StockDep1> batchStocks = stockDep1Repository.findByReferenceAndSourceAndLotNumberStartingWith(
                existing.getReference(), StockSource.PSF, lotPrefix);
        
        for (StockDep1 s : batchStocks) {
            stockDep1Service.deductStock(s.getReference(), s.getLotNumber(), s.getStoreQuantity(), StockSource.PSF);
        }

        globalStockProductService.revertProduction(existing.getReference(), oldNetQty, "PSF-" + existing.getId());
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

    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty())
            return null;
        try {
            return OffsetDateTime.parse(timeStr).toLocalTime();
        } catch (DateTimeParseException e) {
            try {
                return LocalTime.parse(timeStr);
            } catch (DateTimeParseException e2) {
                try {
                    return LocalDateTime.parse(timeStr).toLocalTime();
                } catch (DateTimeParseException e3) {
                    throw new IllegalArgumentException("Invalid time format: " + timeStr);
                }
            }
        }
    }
}
