package com.example.backend.modules.production.departement1.psfProduction.service;

import com.example.backend.modules.admin.product.entity.ProductTypeEnum;
import com.example.backend.modules.admin.usermanagement.entity.User;
import com.example.backend.modules.production.audit.service.OperationAuditService;
import com.example.backend.modules.production.audit.service.OperationAuthorizationService;
import com.example.backend.modules.production.audit.service.OperationEntityTypes;
import com.example.backend.modules.production.departement1.psfProduction.dto.PsfProductionRequest;
import com.example.backend.modules.production.departement1.psfProduction.dto.PsfQrLabelDTO;
import com.example.backend.modules.production.departement1.psfProduction.dto.PsfProductionUpdateResponse;
import com.example.backend.modules.production.departement1.psfProduction.entity.PsfProduction;
import com.example.backend.modules.production.departement1.psfProduction.repository.PsfProductionRepository;
import com.example.backend.modules.production.departement1.shared.entity.StockDep1;
import com.example.backend.modules.production.departement1.shared.service.StockDep1Service;
import com.example.backend.modules.production.productionstock.service.GlobalStockService;
import com.example.backend.modules.production.productionstock.util.QRCodeGenerator;
import com.example.backend.modules.production.shared.util.TimeParser;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PsfProductionService {

    private final PsfProductionRepository productionRepository;
    private final GlobalStockService globalStockProductService;
    private final StockDep1Service stockDep1Service;
    private final com.example.backend.modules.admin.product.service.ProductService masterProductService;
    private final OperationAuthorizationService operationAuthorizationService;
    private final OperationAuditService operationAuditService;
    private final QRCodeGenerator qrCodeGenerator;

    private void validateProductReference(String ref) {
        masterProductService.validateProductReference(ref);
        var product = masterProductService.getProductByRef(ref);
        if (product.getProductType() != ProductTypeEnum.SEMI_FINI) {
            throw new IllegalArgumentException(
                    "Le produit '" + ref + "' n'est pas autorisé pour cette opération. " +
                            "Seuls les semi-finis peuvent être produits par le département PSF.");
        }
    }

    @Transactional
    @SuppressWarnings("null")
    public List<PsfQrLabelDTO> declareProduction(PsfProductionRequest request, User user) {
        validateProductReference(request.getReference());

        if (request.getQuantityPerBatch() <= 0) {
            throw new IllegalArgumentException(messageForUser(
                    user,
                    "La quantité par lot doit être strictement positive."
            ));
        }
        if (request.getQuantity() < 0) {
            throw new IllegalArgumentException(messageForUser(
                    user,
                    "La quantité totale produite ne peut pas être négative."
            ));
        }

    double netQuantity = request.getQuantity() - request.getScrapQuantity();
    int batches = (int) (request.getQuantity() / request.getQuantityPerBatch());
        if (batches <= 0) {
            throw new RuntimeException(messageForUser(
                    user,
                    "La quantité totale produite doit être supérieure à la quantité par lot."
            ));
        }

        LocalTime startTimeParsed = TimeParser.parseTime(request.getStartTime());
        LocalTime endTimeParsed = TimeParser.parseTime(request.getEndTime());

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
                .createdByUsername(user.getUsername())
                .build();



        PsfProduction savedProduction = productionRepository.save(production);
        String stockProductionRef = "PSF-" + savedProduction.getId();
        savedProduction.setStockProductionRef(stockProductionRef);
        productionRepository.save(savedProduction);

        if (request.getScrapQuantity() > request.getQuantity()) {
            throw new IllegalArgumentException(messageForUser(
                    user,
                    "La quantité de rebut ne peut pas dépasser la quantité totale produite."
            ));
        }

    int fullBatches = (int) (netQuantity / request.getQuantityPerBatch());
    double remainder = netQuantity % request.getQuantityPerBatch();
        List<PsfQrLabelDTO> qrCodes = new ArrayList<>();

        for (int i = 0; i < fullBatches; i++) {
            String batchLot = "P-" + savedProduction.getId() + "." + (i + 1);
            stockDep1Service.addStock(
                    request.getReference(),
                    batchLot,
                    ProductTypeEnum.SEMI_FINI,
            request.getQuantityPerBatch());
            qrCodes.add(buildQrLabel(request.getReference(), request.getQuantityPerBatch(), batchLot));
        }
        if (remainder > 0) {
            String batchLot = "P-" + savedProduction.getId() + "." + (fullBatches + 1);
            stockDep1Service.addStock(
                    request.getReference(),
                    batchLot,
                    ProductTypeEnum.SEMI_FINI,
            remainder);
            qrCodes.add(buildQrLabel(request.getReference(), remainder, batchLot));
        }

    globalStockProductService.declareProduction(
        request.getReference(),
        request.getQuantity(),
        stockProductionRef,
        ProductTypeEnum.SEMI_FINI,
        operationAuthorizationService.isAdmin(user));

        if (request.getScrapQuantity() != 0) {
            globalStockProductService.increaseQuantity(
                    request.getReference(),
                    -request.getScrapQuantity(),
                    ProductTypeEnum.SEMI_FINI);
        }

        return qrCodes;
    }

    public List<PsfProduction> getProductionHistory(User user) {
        if (operationAuthorizationService.isAdmin(user)) {
            return productionRepository.findAllByOrderByIdDesc();
        }
        List<PsfProduction> own = productionRepository.findByCreatedByUsernameOrderByIdDesc(user.getUsername());
        if (!own.isEmpty() || user.getMatricule() == null) {
            return own;
        }
        return productionRepository.findByOperatorMatriculeOrderByIdDesc(user.getMatricule());
    }

    public List<StockDep1> getAllStocks() {
        return stockDep1Service.getAllStocks();
    }

    private String messageForUser(User user, String adminMessage) {
        return operationAuthorizationService.isAdmin(user)
                ? adminMessage
                : "Opération impossible.";
    }

    @Transactional
    public PsfProductionUpdateResponse updateProduction(@NonNull Long id, PsfProductionRequest request, User user) {
        PsfProduction existing = productionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Production non trouvée"));

        operationAuthorizationService.assertCanUpdateOperation(
                user,
                existing.getCreatedByUsername(),
                existing.getOperatorMatricule());

        if (!existing.getReference().equals(request.getReference())) {
            validateProductReference(request.getReference());
        }
        if (request.getQuantityPerBatch() <= 0) {
            throw new IllegalArgumentException("La quantité par lot doit être strictement positive.");
        }
        if (request.getQuantity() < 0) {
            throw new IllegalArgumentException("La quantité totale ne peut pas être négative.");
        }

        if (request.getQuantity() < request.getScrapQuantity()) {
            throw new IllegalArgumentException("La quantité de rebut ne peut pas dépasser la quantité totale");
        }

        Map<String, Object> oldSnapshot = snapshot(existing);
        String stockRef = existing.getStockProductionRef() != null
                ? existing.getStockProductionRef()
                : "PSF-" + existing.getId();

        boolean quantityChanged = Double.compare(existing.getQuantity(), request.getQuantity()) != 0;
        boolean referenceChanged = !existing.getReference().equals(request.getReference());
        boolean batchChanged = Double.compare(existing.getQuantityPerBatch(), request.getQuantityPerBatch()) != 0;
        boolean scrapChanged = Double.compare(existing.getScrapQuantity(), request.getScrapQuantity()) != 0;
        boolean shouldAdjustComponents = quantityChanged || referenceChanged;
        boolean shouldAdjustStock = shouldAdjustComponents || batchChanged || scrapChanged;

        if (shouldAdjustStock) {
            double oldNetQuantity = existing.getQuantity() - existing.getScrapQuantity();
            stockDep1Service.deductStock(existing.getReference(), null, oldNetQuantity);
        }

        if (shouldAdjustComponents) {
            globalStockProductService.revertProduction(existing.getReference(), existing.getQuantity(), stockRef);
            if (existing.getScrapQuantity() != 0) {
                globalStockProductService.increaseQuantity(
                        existing.getReference(),
                        existing.getScrapQuantity(),
                        ProductTypeEnum.SEMI_FINI);
            }
        } else if (scrapChanged) {
            double scrapDelta = request.getScrapQuantity() - existing.getScrapQuantity();
            if (scrapDelta != 0) {
                globalStockProductService.increaseQuantity(
                        existing.getReference(),
                        -scrapDelta,
                        ProductTypeEnum.SEMI_FINI);
            }
        }

        existing.setReference(request.getReference());
        existing.setQuantity(request.getQuantity());
        existing.setScrapQuantity(request.getScrapQuantity());
        existing.setQuantityPerBatch(request.getQuantityPerBatch());
        existing.setBatches((int) (request.getQuantity() / request.getQuantityPerBatch()));
        existing.setOperatorMatricule(request.getOperatorMatricule());
        existing.setProducedByCutMachine(request.isProducedByCutMachine());
        LocalTime start = TimeParser.parseTime(request.getStartTime());
        LocalTime end = TimeParser.parseTime(request.getEndTime());
        existing.setStartTime(start);
        existing.setEndTime(end);



        String lotPrefix = "P-" + existing.getId() + ".";
    double netQuantity = request.getQuantity() - request.getScrapQuantity();
    int fullBatches = (int) (netQuantity / request.getQuantityPerBatch());
    double remainder = netQuantity % request.getQuantityPerBatch();
        List<PsfQrLabelDTO> qrLabels = new ArrayList<>();
        for (int i = 0; i < fullBatches; i++) {
            String batchLot = lotPrefix + (i + 1);
            if (shouldAdjustStock) {
                stockDep1Service.addStock(
                        request.getReference(),
                        batchLot,
                        ProductTypeEnum.SEMI_FINI,
                        request.getQuantityPerBatch());
            }
            qrLabels.add(buildQrLabel(request.getReference(), request.getQuantityPerBatch(), batchLot));
        }
        if (remainder > 0) {
            String batchLot = lotPrefix + (fullBatches + 1);
            if (shouldAdjustStock) {
                stockDep1Service.addStock(
                        request.getReference(),
                        batchLot,
                        ProductTypeEnum.SEMI_FINI,
                        remainder);
            }
            qrLabels.add(buildQrLabel(request.getReference(), remainder, batchLot));
        }

    if (shouldAdjustComponents) {
            globalStockProductService.declareProduction(
                    request.getReference(),
                    request.getQuantity(),
                    stockRef,
                    ProductTypeEnum.SEMI_FINI,
                    operationAuthorizationService.isAdmin(user));
    }

    if (shouldAdjustStock && request.getScrapQuantity() != 0) {
        globalStockProductService.increaseQuantity(
            request.getReference(),
            -request.getScrapQuantity(),
            ProductTypeEnum.SEMI_FINI);
        }

        existing.setLastModifiedBy(user.getUsername());

        PsfProduction saved = productionRepository.save(existing);
        operationAuditService.recordUpdate(
                OperationEntityTypes.PSF_PRODUCTION,
                saved.getId(),
                oldSnapshot,
                snapshot(saved),
                user);
    return PsfProductionUpdateResponse.builder()
        .production(saved)
        .qrLabels(qrLabels)
        .build();
    }

    @Transactional
    public void deleteProduction(@NonNull Long id, User user) {
        operationAuthorizationService.assertAdmin(user);
        PsfProduction existing = productionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Production introuvable"));

    double oldQty = existing.getQuantity() - existing.getScrapQuantity();
    String stockRef = existing.getStockProductionRef() != null
        ? existing.getStockProductionRef()
        : "PSF-" + existing.getId();
    stockDep1Service.deductStock(existing.getReference(), null, oldQty);

    globalStockProductService.revertProduction(existing.getReference(), existing.getQuantity(), stockRef);
    if (existing.getScrapQuantity() != 0) {
        globalStockProductService.increaseQuantity(
                existing.getReference(),
                existing.getScrapQuantity(),
                ProductTypeEnum.SEMI_FINI);
    }
        operationAuditService.recordDelete(OperationEntityTypes.PSF_PRODUCTION, existing.getId(), snapshot(existing), user);
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

    private PsfQrLabelDTO buildQrLabel(String reference, double quantity, String lotNumber) {
        String qrContent = qrCodeGenerator.joinFields(reference, quantity, lotNumber);
        byte[] qrImage = qrCodeGenerator.generateQRCodeImage(qrContent, 200, 200);

        return PsfQrLabelDTO.builder()
                .reference(reference)
                .quantity(quantity)
                .lotNumber(lotNumber)
                .qrImageBase64(Base64.getEncoder().encodeToString(qrImage))
                .generatedAt(LocalDateTime.now())
                .build();
    }



    private Map<String, Object> snapshot(PsfProduction production) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", production.getId());
        data.put("reference", production.getReference());
        data.put("quantity", production.getQuantity());
        data.put("quantityPerBatch", production.getQuantityPerBatch());
        data.put("batches", production.getBatches());
        data.put("scrapQuantity", production.getScrapQuantity());
        data.put("operatorMatricule", production.getOperatorMatricule());
        data.put("producedByCutMachine", production.isProducedByCutMachine());
        data.put("startTime", production.getStartTime());
        data.put("endTime", production.getEndTime());
        data.put("lastModifiedAt", production.getLastModifiedAt());
        data.put("lastModifiedBy", production.getLastModifiedBy());
        return data;
    }
}
