package com.example.backend.modules.production.departement1.logisticIncoming.service;

import com.example.backend.modules.admin.product.entity.ProductTypeEnum;
import com.example.backend.modules.admin.usermanagement.entity.User;
import com.example.backend.modules.production.audit.service.OperationAuditService;
import com.example.backend.modules.production.audit.service.OperationAuthorizationService;
import com.example.backend.modules.production.audit.service.OperationEntityTypes;
import com.example.backend.modules.production.departement1.logisticIncoming.dto.IncomingMaterialDTO;
import com.example.backend.modules.production.departement1.logisticIncoming.dto.IncomingUpdateResponse;
import com.example.backend.modules.production.departement1.logisticIncoming.entity.IncomingMaterial;
import com.example.backend.modules.production.departement1.logisticIncoming.repository.IncomingMaterialRepository;
import com.example.backend.modules.production.departement1.shared.entity.StockDep1;
import com.example.backend.modules.production.departement1.shared.service.StockDep1Service;
import com.example.backend.modules.production.productionstock.service.GlobalStockService;
import com.example.backend.modules.production.productionstock.util.QRCodeGenerator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LogisticIncomingService {

    private final IncomingMaterialRepository incomingRepository;
    private final StockDep1Service stockDep1Service;
    private final GlobalStockService globalStockProductService;
    private final com.example.backend.modules.admin.product.service.ProductService masterProductService;
    private final OperationAuthorizationService operationAuthorizationService;
    private final OperationAuditService operationAuditService;
    private final QRCodeGenerator qrCodeGenerator;

    private void validateProductReference(String ref) {
        masterProductService.validateProductReference(ref);
        var product = masterProductService.getProductByRef(ref);
        if (product.getProductType() != ProductTypeEnum.MATIERE_PREMIERE) {
            throw new IllegalArgumentException(
                    "Le produit '" + ref + "' n'est pas autorisé pour cette opération. " +
                            "Seules les matières premières peuvent être enregistrées comme entrées.");
        }
    }

    @Transactional
    public byte[] registerIncoming(IncomingMaterialDTO request, User user) {
        validateProductReference(request.getReference());
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("La quantité doit être strictement positive.");
        }

        String requestedLotNumber = normalizeLotNumber(request.getLotNumber());

        IncomingMaterial incoming = IncomingMaterial.builder()
                .reference(request.getReference())
                .quantity(request.getQuantity())
                .lotNumber(requestedLotNumber != null ? requestedLotNumber : "TEMP")
                .operationDate(request.getOperationDate() != null ? request.getOperationDate() : LocalDateTime.now())
                .createdByUsername(user.getUsername())
                .build();

        incoming = incomingRepository.save(incoming);

        String finalLotNumber = requestedLotNumber != null ? requestedLotNumber : "LOT-" + incoming.getId();
        if (!finalLotNumber.equals(incoming.getLotNumber())) {
            incoming.setLotNumber(finalLotNumber);
            incoming = incomingRepository.save(incoming);
        }

        stockDep1Service.addStock(
                request.getReference(),
                finalLotNumber,
                ProductTypeEnum.MATIERE_PREMIERE,
        request.getQuantity());
        globalStockProductService.increaseQuantity(
                request.getReference(),
                request.getQuantity(),
                ProductTypeEnum.MATIERE_PREMIERE);

        if (Boolean.TRUE.equals(request.getGenerateQr())) {
            String qrContent = qrCodeGenerator.joinFields(request.getReference(), request.getQuantity(), finalLotNumber);
            return qrCodeGenerator.generateQRCodeImage(qrContent, 200, 200);
        }

        return null;
    }

    public List<IncomingMaterial> getIncomingHistory(User user) {
        if (operationAuthorizationService.isAdmin(user)) {
            return incomingRepository.findAllByOrderByIdDesc();
        }

        List<IncomingMaterial> own = incomingRepository.findByCreatedByUsernameOrderByIdDesc(user.getUsername());
        return own;
    }

    public List<StockDep1> getAllStocks() {
        return stockDep1Service.getAllStocks();
    }

    @Transactional
    public IncomingUpdateResponse updateIncoming(Long id, IncomingMaterialDTO request, User user) {
        IncomingMaterial existing = incomingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Enregistrement d'entrée non trouvé"));

        operationAuthorizationService.assertCanUpdateOperation(
                user,
                existing.getCreatedByUsername(),
        null);

        String nextReference = request.getReference() != null ? request.getReference() : existing.getReference();
        String nextLotNumber = (request.getLotNumber() != null && !request.getLotNumber().isBlank())
                ? request.getLotNumber()
                : existing.getLotNumber();
        Double nextQuantity = request.getQuantity() != null ? request.getQuantity() : existing.getQuantity();

        if (nextQuantity == null || nextQuantity <= 0) {
            throw new IllegalArgumentException("La quantité doit être strictement positive.");
        }
        if (!existing.getReference().equals(nextReference)) {
            validateProductReference(nextReference);
        }

        Map<String, Object> oldSnapshot = snapshot(existing);

        stockDep1Service.deductStock(
                existing.getReference(),
                existing.getLotNumber(),
        existing.getQuantity());
        globalStockProductService.increaseQuantity(
                existing.getReference(),
                -existing.getQuantity(),
                ProductTypeEnum.MATIERE_PREMIERE);

        existing.setReference(nextReference);
        existing.setQuantity(nextQuantity);
        existing.setLotNumber(nextLotNumber);
        if (request.getOperationDate() != null) {
            existing.setOperationDate(request.getOperationDate());
        }
        existing.setLastModifiedBy(user.getUsername());

        stockDep1Service.addStock(
                existing.getReference(),
                existing.getLotNumber(),
                ProductTypeEnum.MATIERE_PREMIERE,
        existing.getQuantity());
        globalStockProductService.increaseQuantity(
                existing.getReference(),
                existing.getQuantity(),
                ProductTypeEnum.MATIERE_PREMIERE);

        IncomingMaterial saved = incomingRepository.save(existing);
        operationAuditService.recordUpdate(
                OperationEntityTypes.INCOMING,
                saved.getId(),
                oldSnapshot,
                snapshot(saved),
                user);

    byte[] qrImage = generateQRCode(saved.getReference(), saved.getQuantity(), saved.getLotNumber());
    return IncomingUpdateResponse.builder()
        .incoming(saved)
        .qrImageBase64(Base64.getEncoder().encodeToString(qrImage))
        .reference(saved.getReference())
        .quantity(saved.getQuantity())
        .lotNumber(saved.getLotNumber())
        .generatedAt(LocalDateTime.now())
        .build();
    }

    @Transactional
    public void deleteIncoming(Long id, User user) {
        operationAuthorizationService.assertAdmin(user);
        IncomingMaterial existing = incomingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Enregistrement d'entrée introuvable"));

        stockDep1Service.deductStock(
                existing.getReference(),
                existing.getLotNumber(),
        existing.getQuantity());
        globalStockProductService.increaseQuantity(
                existing.getReference(),
                -existing.getQuantity(),
                ProductTypeEnum.MATIERE_PREMIERE);

        operationAuditService.recordDelete(OperationEntityTypes.INCOMING, existing.getId(), snapshot(existing), user);
        incomingRepository.delete(existing);
    }

    @Transactional
    public StockDep1 updateStock(Long id, StockDep1 request) {
        return stockDep1Service.updateStock(id, request);
    }

    @Transactional
    public void deleteStock(Long id) {
        stockDep1Service.deleteStock(id);
    }

    public byte[] generateQRCode(String reference, Double quantity, String lotNumber) {
        String qrContent = qrCodeGenerator.joinFields(reference, quantity, lotNumber);
        return qrCodeGenerator.generateQRCodeImage(qrContent, 200, 200);
    }

    private String normalizeLotNumber(String lotNumber) {
        if (lotNumber == null) {
            return null;
        }
        String trimmed = lotNumber.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<String, Object> snapshot(IncomingMaterial incoming) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", incoming.getId());
        data.put("reference", incoming.getReference());
        data.put("quantity", incoming.getQuantity());
        data.put("lotNumber", incoming.getLotNumber());
        data.put("operationDate", incoming.getOperationDate());
        data.put("lastModifiedAt", incoming.getLastModifiedAt());
        data.put("lastModifiedBy", incoming.getLastModifiedBy());
        return data;
    }
}
