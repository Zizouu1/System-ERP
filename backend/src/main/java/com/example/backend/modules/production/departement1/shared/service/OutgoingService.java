package com.example.backend.modules.production.departement1.shared.service;

import com.example.backend.modules.admin.product.entity.ProductTypeEnum;
import com.example.backend.modules.admin.usermanagement.entity.Role;
import com.example.backend.modules.admin.usermanagement.entity.User;
import com.example.backend.modules.production.audit.service.OperationAuditService;
import com.example.backend.modules.production.audit.service.OperationAuthorizationService;
import com.example.backend.modules.production.audit.service.OperationEntityTypes;
import com.example.backend.modules.production.departement1.shared.dto.OutgoingDTO;
import com.example.backend.modules.production.departement1.shared.entity.Outgoing;
import com.example.backend.modules.production.departement1.shared.entity.StockDep1;
import com.example.backend.modules.production.departement1.shared.repository.OutgoingRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OutgoingService {

    private final OutgoingRepository outgoingRepository;
    private final StockDep1Service stockDep1Service;
    private final com.example.backend.modules.admin.product.service.ProductService masterProductService;
    private final OperationAuthorizationService operationAuthorizationService;
    private final OperationAuditService operationAuditService;

    @Transactional
    public Outgoing registerOutgoing(OutgoingDTO request, User user) {
        masterProductService.validateProductReference(request.getReference());
        validateOutgoingReferenceForRole(user.getRole(), request.getReference());

        StockDep1 stock = findMatchingStock(request.getReference(), request.getLotNumber());
        double quantity = requirePositiveQuantity(request.getQuantityOut());

        if (quantity > stock.getStoreQuantity()) {
            throw new IllegalArgumentException("Insufficient stock. Available: " + stock.getStoreQuantity());
        }

        Outgoing outgoing = Outgoing.builder()
                .reference(request.getReference())
                .lotNumber(request.getLotNumber())
                .quantityOut(quantity)
                .operationDate(request.getOperationDate() != null ? request.getOperationDate() : LocalDateTime.now())
                .notes(request.getNotes())
                .createdByUsername(user.getUsername())
                .build();

        Outgoing saved = outgoingRepository.save(outgoing);
        stockDep1Service.consumeStoreOnly(
                stock.getReference(),
        request.getLotNumber(),
        quantity);
        return saved;
    }

    @Transactional
    public Outgoing updateOutgoing(Long id, OutgoingDTO request, User user) {
        Outgoing outgoing = outgoingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Outgoing material not found with ID: " + id));

        operationAuthorizationService.assertCanUpdateOperation(user, outgoing.getCreatedByUsername(), null);

        String reference = request.getReference() != null ? request.getReference() : outgoing.getReference();
        String lotNumber = request.getLotNumber() != null ? request.getLotNumber() : outgoing.getLotNumber();
        double requestedQuantity = request.getQuantityOut() != null ? request.getQuantityOut() : outgoing.getQuantityOut();
        if (requestedQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        validateOutgoingReferenceForRole(user.getRole(), reference);

        Map<String, Object> oldSnapshot = snapshot(outgoing);

        StockDep1 oldStock = findMatchingStock(outgoing.getReference(), outgoing.getLotNumber());
        stockDep1Service.restoreStoreOnly(
                oldStock.getReference(),
        outgoing.getLotNumber(),
        outgoing.getQuantityOut());

        StockDep1 newStock = findMatchingStock(reference, lotNumber);
        if (newStock.getStoreQuantity() < requestedQuantity) {
            throw new IllegalArgumentException("Insufficient stock. Available: " + newStock.getStoreQuantity());
        }
        stockDep1Service.consumeStoreOnly(
                newStock.getReference(),
        lotNumber,
        requestedQuantity);

        outgoing.setReference(reference);
        outgoing.setLotNumber(lotNumber);
        outgoing.setQuantityOut(requestedQuantity);
        if (request.getNotes() != null) {
            outgoing.setNotes(request.getNotes());
        }
        if (request.getOperationDate() != null) {
            outgoing.setOperationDate(request.getOperationDate());
        }
        outgoing.setModified(true);
        outgoing.setLastModifiedAt(LocalDateTime.now());
        outgoing.setLastModifiedBy(user.getUsername());

        Outgoing saved = outgoingRepository.save(outgoing);
        operationAuditService.recordUpdate(OperationEntityTypes.OUTGOING, saved.getId(), oldSnapshot, snapshot(saved), user);
        return saved;
    }

    @Transactional
    public void deleteOutgoing(Long id, User user) {
        operationAuthorizationService.assertAdmin(user);
        Outgoing outgoing = outgoingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Outgoing material not found with ID: " + id));

        StockDep1 stock = findMatchingStock(outgoing.getReference(), outgoing.getLotNumber());
        stockDep1Service.restoreStoreOnly(
                stock.getReference(),
        outgoing.getLotNumber(),
        outgoing.getQuantityOut());

        operationAuditService.recordDelete(OperationEntityTypes.OUTGOING, outgoing.getId(), snapshot(outgoing), user);
        outgoingRepository.delete(outgoing);
    }

    public List<Outgoing> getOutgoingHistory(User user) {
        List<Outgoing> base = operationAuthorizationService.isAdmin(user)
                ? outgoingRepository.findAllByOrderByIdDesc()
                : outgoingRepository.findByCreatedByUsernameOrderByIdDesc(user.getUsername());

        if (operationAuthorizationService.isAdmin(user)) {
            return base;
        }
        if (user.getRole() == Role.LOGISTIC) {
            return base.stream()
                    .filter(outgoing -> productTypeOf(outgoing.getReference()) == ProductTypeEnum.MATIERE_PREMIERE)
                    .toList();
        }
        if (user.getRole() == Role.PSF) {
            return base.stream()
                    .filter(outgoing -> productTypeOf(outgoing.getReference()) == ProductTypeEnum.SEMI_FINI)
                    .toList();
        }
        return List.of();
    }

    private void validateOutgoingReferenceForRole(Role role, String reference) {
        if (role == Role.ADMIN) {
            return;
        }
        ProductTypeEnum type = productTypeOf(reference);
        if (role == Role.LOGISTIC && type != ProductTypeEnum.MATIERE_PREMIERE) {
            throw new IllegalArgumentException(
                    "La logistique ne peut enregistrer que des sorties de matières premières.");
        }
        if (role == Role.PSF && type != ProductTypeEnum.SEMI_FINI) {
            throw new IllegalArgumentException(
                    "Le département PSF ne peut enregistrer que des sorties de semi-finis.");
        }
    }

    private ProductTypeEnum productTypeOf(String reference) {
        return masterProductService.getProductByRef(reference).getProductType();
    }

    private double requirePositiveQuantity(Double quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        return quantity;
    }

    private StockDep1 findMatchingStock(String reference, String lotNumber) {
    return stockDep1Service.findStockByReferenceAndLot(reference, lotNumber)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Stock not found for reference " + reference + " and lot " + lotNumber));
    }

    private Map<String, Object> snapshot(Outgoing outgoing) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", outgoing.getId());
        data.put("reference", outgoing.getReference());
        data.put("lotNumber", outgoing.getLotNumber());
        data.put("quantityOut", outgoing.getQuantityOut());
        data.put("operationDate", outgoing.getOperationDate());
        data.put("notes", outgoing.getNotes());
        data.put("modified", Boolean.TRUE.equals(outgoing.getModified()));
        data.put("lastModifiedAt", outgoing.getLastModifiedAt());
        data.put("lastModifiedBy", outgoing.getLastModifiedBy());
        return data;
    }
}
