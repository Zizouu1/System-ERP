package com.example.backend.modules.production.departement2.service;

import com.example.backend.modules.admin.product.entity.ProductTypeEnum;
import com.example.backend.modules.admin.usermanagement.entity.User;
import com.example.backend.modules.production.audit.service.OperationAuditService;
import com.example.backend.modules.production.audit.service.OperationAuthorizationService;
import com.example.backend.modules.production.audit.service.OperationEntityTypes;
import com.example.backend.modules.production.departement2.dto.PfProductionRequest;
import com.example.backend.modules.production.departement2.entity.PfProduction;
import com.example.backend.modules.production.departement2.repository.PfProductionRepository;
import com.example.backend.modules.production.productionstock.service.GlobalStockService;
import com.example.backend.modules.production.shared.util.TimeParser;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PfProductionService {

    private final PfProductionRepository pfProductionRepository;
    private final GlobalStockService globalStockProductService;
    private final com.example.backend.modules.admin.product.service.ProductService masterProductService;
    private final OperationAuthorizationService operationAuthorizationService;
    private final OperationAuditService operationAuditService;

    private void validateProductReference(String ref) {
        masterProductService.validateProductReference(ref);
        var product = masterProductService.getProductByRef(ref);
        if (!"produit fini".equalsIgnoreCase(product.getProductType().getDisplayName())) {
            throw new IllegalArgumentException("Le produit '" + ref + "' doit être de type 'produit fini'.");
        }
    }

    @Transactional
    @SuppressWarnings("null")
    public PfProduction saveProduction(PfProductionRequest request, User user) {
        validateProductReference(request.getReference());
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new IllegalArgumentException("L'heure de début et de fin sont obligatoires.");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("La quantité doit être supérieure à zéro.");
        }

        LocalTime startTimeParsed = TimeParser.parseTime(request.getStartTime());
        LocalTime endTimeParsed = TimeParser.parseTime(request.getEndTime());

        PfProduction production = PfProduction.builder()
                .operatorMatricule(request.getOperatorMatricule())
                .reference(request.getReference())
        .quantity(request.getQuantity().doubleValue())
                .scrapQuantity(request.getScrapQuantity())
                .startTime(startTimeParsed)
                .endTime(endTimeParsed)
                .createdByUsername(user.getUsername())
                .build();

    PfProduction saved = pfProductionRepository.save(production);
        if (request.getScrapQuantity() > request.getQuantity()) {
        throw new IllegalArgumentException("La quantité de rebut ne peut pas dépasser la quantité totale.");
    }

        double scrapQuantity = request.getScrapQuantity();

    globalStockProductService.declareProduction(
        request.getReference(),
        request.getQuantity(),
        productionRef(saved.getId()),
        ProductTypeEnum.PRODUIT_FINI,
        operationAuthorizationService.isAdmin(user));

        if (scrapQuantity != 0) {
            globalStockProductService.increaseQuantity(
                    request.getReference(),
                    -scrapQuantity,
                    ProductTypeEnum.PRODUIT_FINI);
        }

        return saved;
    }

    public List<PfProduction> getProductionHistory(User user) {
        if (operationAuthorizationService.isAdmin(user)) {
            return pfProductionRepository.findAllByOrderByIdDesc();
        }
        List<PfProduction> own = pfProductionRepository.findByCreatedByUsernameOrderByIdDesc(user.getUsername());
        if (!own.isEmpty() || user.getMatricule() == null) {
            return own;
        }
        return pfProductionRepository.findByOperatorMatriculeOrderByIdDesc(user.getMatricule());
    }

    @Transactional
    public PfProduction updateProduction(@NonNull Long id, PfProductionRequest request, User user) {
        validateProductReference(request.getReference());
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("La quantité doit être supérieure à zéro.");
        }
        PfProduction existing = pfProductionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Production non trouvée"));

        operationAuthorizationService.assertCanUpdateOperation(
                user,
                existing.getCreatedByUsername(),
                existing.getOperatorMatricule());

        Map<String, Object> oldSnapshot = snapshot(existing);

        if (request.getScrapQuantity() > request.getQuantity()) {
            throw new IllegalArgumentException("La quantité de rebut ne peut pas dépasser la quantité totale.");
        }

        boolean quantityChanged = existing.getQuantity() == null
                || Double.compare(existing.getQuantity(), request.getQuantity()) != 0;
        boolean referenceChanged = !existing.getReference().equals(request.getReference());
        boolean shouldAdjustStock = quantityChanged || referenceChanged;

    if (shouldAdjustStock) {
        double oldQuantity = existing.getQuantity() != null ? existing.getQuantity() : 0.0;
        globalStockProductService.revertProduction(
            existing.getReference(),
            oldQuantity,
            productionRef(existing.getId()));
        if (existing.getScrapQuantity() != 0) {
            globalStockProductService.increaseQuantity(
                    existing.getReference(),
                    existing.getScrapQuantity(),
                    ProductTypeEnum.PRODUIT_FINI);
        }
    }

        existing.setReference(request.getReference());
        existing.setOperatorMatricule(request.getOperatorMatricule());
    existing.setQuantity(request.getQuantity().doubleValue());
        existing.setScrapQuantity(request.getScrapQuantity());

        if (request.getStartTime() != null && request.getEndTime() != null) {
            LocalTime start = TimeParser.parseTime(request.getStartTime());
            LocalTime end = TimeParser.parseTime(request.getEndTime());
            existing.setStartTime(start);
            existing.setEndTime(end);
        }

    if (shouldAdjustStock) {
        globalStockProductService.declareProduction(
            request.getReference(),
            request.getQuantity(),
            productionRef(existing.getId()),
            ProductTypeEnum.PRODUIT_FINI,
            operationAuthorizationService.isAdmin(user));
        if (request.getScrapQuantity() != 0) {
            globalStockProductService.increaseQuantity(
                    request.getReference(),
                    -request.getScrapQuantity(),
                    ProductTypeEnum.PRODUIT_FINI);
        }
    }

        existing.setLastModifiedBy(user.getUsername());

        PfProduction saved = pfProductionRepository.save(existing);
        operationAuditService.recordUpdate(
                OperationEntityTypes.PF_PRODUCTION,
                saved.getId(),
                oldSnapshot,
                snapshot(saved),
                user);
        return saved;
    }

    @Transactional
    public void deleteProduction(@NonNull Long id, User user) {
        operationAuthorizationService.assertAdmin(user);
        PfProduction existing = pfProductionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Production introuvable"));

    double oldQty = existing.getQuantity() != null ? existing.getQuantity() : 0.0;
    globalStockProductService.revertProduction(existing.getReference(), oldQty, productionRef(existing.getId()));
    if (existing.getScrapQuantity() != 0) {
        globalStockProductService.increaseQuantity(
                existing.getReference(),
                existing.getScrapQuantity(),
                ProductTypeEnum.PRODUIT_FINI);
    }
        operationAuditService.recordDelete(OperationEntityTypes.PF_PRODUCTION, existing.getId(), snapshot(existing), user);
        pfProductionRepository.delete(existing);
    }

    private String productionRef(Long id) {
        return "PF-" + id;
    }



    private Map<String, Object> snapshot(PfProduction production) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", production.getId());
        data.put("operatorMatricule", production.getOperatorMatricule());
        data.put("reference", production.getReference());
        data.put("quantity", production.getQuantity());
        data.put("scrapQuantity", production.getScrapQuantity());
        data.put("startTime", production.getStartTime());
        data.put("endTime", production.getEndTime());
    data.put("startDateTime", production.getStartDateTime());
    data.put("endDateTime", production.getEndDateTime());
        data.put("createdAt", production.getCreatedAt());
        data.put("lastModifiedAt", production.getLastModifiedAt());
        data.put("lastModifiedBy", production.getLastModifiedBy());
        return data;
    }
}
