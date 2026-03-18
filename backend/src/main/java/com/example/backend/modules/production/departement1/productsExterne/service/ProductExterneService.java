package com.example.backend.modules.production.departement1.productsExterne.service;

import com.example.backend.modules.admin.usermanagement.entity.User;
import com.example.backend.modules.admin.product.entity.ProductTypeEnum;
import com.example.backend.modules.production.departement1.productsExterne.dto.IncomingMaterialDTO;
import com.example.backend.modules.production.departement1.productsExterne.entity.IncomingMaterial;
import com.example.backend.modules.production.departement1.productsExterne.repository.IncomingMaterialRepository;
import com.example.backend.modules.production.departement1.shared.entity.StockDep1;
import com.example.backend.modules.production.departement1.shared.entity.StockSource;
import com.example.backend.modules.production.departement1.shared.service.StockDep1Service;
import com.example.backend.modules.production.productionstock.service.ProductService;
import com.example.backend.modules.production.productionstock.util.QRCodeGenerator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductExterneService {

        private final IncomingMaterialRepository incomingRepository;
        private final StockDep1Service stockDep1Service;
        private final ProductService globalStockProductService;
        private final com.example.backend.modules.admin.product.service.ProductService masterProductService;
        private final QRCodeGenerator qrCodeGenerator;

        /**
         * Validates that a reference exists in the Product master table.
         * Ensures the product type is MATIERE_PREMIERE for incoming materials.
         * Throws exception if ref not found or product type is incorrect.
         */
        private void validateProductReference(String ref) {
                masterProductService.validateProductReference(ref);
                var product = masterProductService.getProductByRef(ref);
                // Validate that the product type is MATIERE_PREMIERE for incoming materials
                if (product.getProductType() != ProductTypeEnum.MATIERE_PREMIERE) {
                        throw new IllegalArgumentException(
                                "Le produit '" + ref + "' n'est pas autorisé pour cette opération. " +
                                "Seules les matières premières peuvent être enregistrées comme entrées.");
                }
        }

        @Transactional
        public byte[] registerIncoming(IncomingMaterialDTO request, User user) {
                // Validate product reference exists in Product master
                validateProductReference(request.getReference());

                // Save first to get the auto-generated ID
                IncomingMaterial incoming = IncomingMaterial.builder()
                                .reference(request.getReference())
                                .quantity(request.getQuantity())
                                .lotNumber("TEMP") // Temporary lot number
                                .operationDate(LocalDateTime.now())
                                .operatorMatricule(request.getOperatorMatricule())
                                .notes(request.getNotes())
                                .build();

                incoming = incomingRepository.save(incoming);

                // Now set the actual lot number based on the ID
                String lotNumber = "LOT-" + incoming.getId();
                incoming.setLotNumber(lotNumber);
                incomingRepository.save(incoming);

                // Add to stock with the generated lot number
                stockDep1Service.addStock(request.getReference(), lotNumber, ProductTypeEnum.MATIERE_PREMIERE,
                                request.getQuantity(),
                                StockSource.LOGISTIC);

                globalStockProductService.increaseQuantity(request.getReference(), request.getQuantity(), ProductTypeEnum.MATIERE_PREMIERE);

                String qrContent = qrCodeGenerator.joinFields(request.getReference(), request.getQuantity(), lotNumber);
                return qrCodeGenerator.generateQRCodeImage(qrContent, 200, 200);
        }

        public List<IncomingMaterial> getAllIncoming() {
                return incomingRepository.findAll();
        }

        public List<StockDep1> getAllStocks() {
                return stockDep1Service.getAllStockBySource(null);
        }

        @Transactional
        public IncomingMaterial updateIncoming(Long id, IncomingMaterialDTO request) {
                IncomingMaterial existing = incomingRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Enregistrement d'entrée non trouvé"));

                // Validate the new reference if it has changed
                if (!existing.getReference().equals(request.getReference())) {
                        validateProductReference(request.getReference());
                }

                stockDep1Service.deductStock(existing.getReference(), existing.getLotNumber(), existing.getQuantity(),
                                StockSource.LOGISTIC);
                globalStockProductService.increaseQuantity(existing.getReference(), -existing.getQuantity(), ProductTypeEnum.MATIERE_PREMIERE);

                existing.setReference(request.getReference());
                existing.setQuantity(request.getQuantity());
                existing.setLotNumber(request.getLotNumber());
                existing.setOperatorMatricule(request.getOperatorMatricule());
                existing.setNotes(request.getNotes());
                if (request.getOperationDate() != null) {
                        existing.setOperationDate(request.getOperationDate());
                }

                stockDep1Service.addStock(request.getReference(), request.getLotNumber(), ProductTypeEnum.MATIERE_PREMIERE,
                                request.getQuantity(), StockSource.LOGISTIC);
                globalStockProductService.increaseQuantity(request.getReference(), request.getQuantity(), ProductTypeEnum.MATIERE_PREMIERE);

                return incomingRepository.save(existing);
        }

        @Transactional
        public void deleteIncoming(Long id) {
                IncomingMaterial existing = incomingRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Enregistrement d'entrée introuvable"));

                stockDep1Service.deductStock(existing.getReference(), existing.getLotNumber(), existing.getQuantity(),
                                StockSource.LOGISTIC);
                globalStockProductService.increaseQuantity(existing.getReference(), -existing.getQuantity(), ProductTypeEnum.MATIERE_PREMIERE);

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
}
