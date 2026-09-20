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
import com.example.backend.modules.admin.nomenclature.entity.Nomenclature;
import com.example.backend.modules.admin.nomenclature.repository.NomenclatureRepository;
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
    private final NomenclatureRepository nomenclatureRepository;

    @Transactional
    public Outgoing registerOutgoing(OutgoingDTO request, User user) {
        masterProductService.validateProductReference(request.getReference());
        validateOutgoingReferenceForRole(user.getRole(), request.getReference());

        StockDep1 stock = findMatchingStock(request.getReference(), request.getLotNumber(), user);
        double quantity = requirePositiveQuantity(request.getQuantityOut(), user);

        if (quantity > stock.getStoreQuantity()) {
            throw new IllegalArgumentException(messageForUser(
                    user,
                    "Stock insuffisant. Disponible : " + stock.getStoreQuantity()
            ));
        }

        Outgoing outgoing = Outgoing.builder()
                .reference(request.getReference())
                .lotNumber(request.getLotNumber())
                .quantityOut(quantity)
                .operationDate(request.getOperationDate() != null ? request.getOperationDate() : LocalDateTime.now())
                .createdByUsername(user.getUsername())
                .build();

    Outgoing saved = outgoingRepository.save(outgoing);
    stockDep1Service.consumeStoreOnly(
        stock.getReference(),
        request.getLotNumber(),
        quantity);
        
        if (productTypeOf(request.getReference()) == ProductTypeEnum.SEMI_FINI) {
            consumeBomComponents(request.getReference(), quantity, false);
        }
        
        return saved;
    }

    @Transactional
    public Outgoing updateOutgoing(Long id, OutgoingDTO request, User user) {
        Outgoing outgoing = outgoingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageForUser(
                        user,
                        "Sortie introuvable avec l'identifiant : " + id
                )));

        operationAuthorizationService.assertCanUpdateOperation(user, outgoing.getCreatedByUsername(), null);

        String reference = request.getReference() != null ? request.getReference() : outgoing.getReference();
        String lotNumber = request.getLotNumber() != null ? request.getLotNumber() : outgoing.getLotNumber();
        double requestedQuantity = request.getQuantityOut() != null ? request.getQuantityOut() : outgoing.getQuantityOut();
        if (requestedQuantity <= 0) {
            throw new IllegalArgumentException(messageForUser(
                    user,
                    "La quantité doit être strictement positive."
            ));
        }

        validateOutgoingReferenceForRole(user.getRole(), reference);

        Map<String, Object> oldSnapshot = snapshot(outgoing);

        StockDep1 oldStock = findMatchingStock(outgoing.getReference(), outgoing.getLotNumber(), user);
        stockDep1Service.restoreStoreOnly(
                oldStock.getReference(),
                outgoing.getLotNumber(),
                outgoing.getQuantityOut());

        if (productTypeOf(outgoing.getReference()) == ProductTypeEnum.SEMI_FINI) {
            consumeBomComponents(outgoing.getReference(), outgoing.getQuantityOut(), true);
        }

        StockDep1 newStock = findMatchingStock(reference, lotNumber, user);
        if (newStock.getStoreQuantity() < requestedQuantity) {
            throw new IllegalArgumentException(messageForUser(
                    user,
                    "Stock insuffisant. Disponible : " + newStock.getStoreQuantity()
            ));
        }
        stockDep1Service.consumeStoreOnly(
                newStock.getReference(),
                lotNumber,
                requestedQuantity);

        if (productTypeOf(reference) == ProductTypeEnum.SEMI_FINI) {
            consumeBomComponents(reference, requestedQuantity, false);
        }

        outgoing.setReference(reference);
        outgoing.setLotNumber(lotNumber);
        outgoing.setQuantityOut(requestedQuantity);
        if (request.getOperationDate() != null) {
            outgoing.setOperationDate(request.getOperationDate());
        }
        outgoing.setLastModifiedBy(user.getUsername());

        Outgoing saved = outgoingRepository.save(outgoing);
        operationAuditService.recordUpdate(OperationEntityTypes.OUTGOING, saved.getId(), oldSnapshot, snapshot(saved), user);
        return saved;
    }

    @Transactional
    public void deleteOutgoing(Long id, User user) {
        operationAuthorizationService.assertAdmin(user);
        Outgoing outgoing = outgoingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Sortie introuvable avec l'identifiant : " + id
                ));

        StockDep1 stock = findMatchingStock(outgoing.getReference(), outgoing.getLotNumber(), user);
        stockDep1Service.restoreStoreOnly(
                stock.getReference(),
                outgoing.getLotNumber(),
                outgoing.getQuantityOut());

        if (productTypeOf(outgoing.getReference()) == ProductTypeEnum.SEMI_FINI) {
            consumeBomComponents(outgoing.getReference(), outgoing.getQuantityOut(), true);
        }

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

    private double requirePositiveQuantity(Double quantity, User user) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException(messageForUser(
                    user,
                    "La quantité doit être strictement positive."
            ));
        }
        return quantity;
    }

    private StockDep1 findMatchingStock(String reference, String lotNumber, User user) {
        return stockDep1Service.findStockByReferenceAndLot(reference, lotNumber)
                .orElseThrow(() -> new EntityNotFoundException(messageForUser(
                        user,
                        "Stock introuvable pour la référence " + reference + " et le lot " + lotNumber
                )));
    }

    private String messageForUser(User user, String adminMessage) {
        return operationAuthorizationService.isAdmin(user)
                ? adminMessage
                : "Opération impossible.";
    }

    private Map<String, Object> snapshot(Outgoing outgoing) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", outgoing.getId());
        data.put("reference", outgoing.getReference());
        data.put("lotNumber", outgoing.getLotNumber());
        data.put("quantityOut", outgoing.getQuantityOut());
        data.put("operationDate", outgoing.getOperationDate());
        data.put("lastModifiedAt", outgoing.getLastModifiedAt());
        data.put("lastModifiedBy", outgoing.getLastModifiedBy());
        return data;
    }

    private void consumeBomComponents(String parentRef, double parentQuantity, boolean isReversal) {
        List<Nomenclature> bom = nomenclatureRepository.findByParentRef(parentRef);
        for (Nomenclature entry : bom) {
            String componentRef = entry.getComponentRef();
            double requiredQuantity = entry.getQuantityRequired() * parentQuantity;

            // Recurse first (depth-first)
            consumeBomComponents(componentRef, requiredQuantity, isReversal);

            if (isReversal) {
                stockDep1Service.restoreStoreOnly(componentRef, null, requiredQuantity);
            } else {
                stockDep1Service.consumeStoreOnly(componentRef, null, requiredQuantity);
            }
        }
    }
}
