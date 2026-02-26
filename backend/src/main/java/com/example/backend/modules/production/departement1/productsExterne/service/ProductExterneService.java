package com.example.backend.modules.production.departement1.productsExterne.service;

import com.example.backend.modules.admin.usermanagement.entity.User;
import com.example.backend.modules.production.departement1.productsExterne.dto.IncomingMaterialDTO;
import com.example.backend.modules.production.departement1.productsExterne.entity.IncomingMaterial;
import com.example.backend.modules.production.departement1.productsExterne.repository.IncomingMaterialRepository;
import com.example.backend.modules.production.departement1.shared.entity.StockDep1;
import com.example.backend.modules.production.departement1.shared.entity.StockSource;
import com.example.backend.modules.production.departement1.shared.service.StockDep1Service;
import com.example.backend.modules.production.productionstock.entity.ProductType;
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
        private final ProductService productService;
        private final QRCodeGenerator qrCodeGenerator;

        @Transactional
        public byte[] registerIncoming(IncomingMaterialDTO request, User user) {
                String lotNumber = (request.getLotNumber() != null && !request.getLotNumber().isEmpty())
                                ? request.getLotNumber()
                                : "EXT-" + UUID.randomUUID().toString();

                stockDep1Service.addStock(request.getReference(), lotNumber, ProductType.RAW_MATERIAL,
                                request.getQuantity(),
                                StockSource.LOGISTIC);

                IncomingMaterial incoming = IncomingMaterial.builder()
                                .reference(request.getReference())
                                .quantity(request.getQuantity())
                                .lotNumber(lotNumber)
                                .operationDate(LocalDateTime.now())
                                .operatorMatricule(request.getOperatorMatricule())
                                .notes(request.getNotes())
                                .build();

                incomingRepository.save(incoming);
                productService.increaseQuantity(request.getReference(), request.getQuantity());

                String qrContent = qrCodeGenerator.joinFields(request.getReference(), request.getQuantity(), lotNumber);
                return qrCodeGenerator.generateQRCodeImage(qrContent, 200, 200);
        }

        public List<IncomingMaterial> getAllIncoming() {
                return incomingRepository.findAll();
        }

        public List<StockDep1> getAllStocks() {
                return stockDep1Service.getAllStockBySource(StockSource.LOGISTIC);
        }

        @Transactional
        public IncomingMaterial updateIncoming(Long id, IncomingMaterialDTO request) {
                IncomingMaterial existing = incomingRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Incoming record not found"));

                stockDep1Service.deductStock(existing.getReference(), existing.getLotNumber(), existing.getQuantity(),
                                StockSource.LOGISTIC);
                productService.increaseQuantity(existing.getReference(), -existing.getQuantity());

                existing.setReference(request.getReference());
                existing.setQuantity(request.getQuantity());
                existing.setLotNumber(request.getLotNumber());
                existing.setOperatorMatricule(request.getOperatorMatricule());
                existing.setNotes(request.getNotes());
                if (request.getOperationDate() != null) {
                        existing.setOperationDate(request.getOperationDate());
                }

                stockDep1Service.addStock(request.getReference(), request.getLotNumber(), ProductType.RAW_MATERIAL,
                                request.getQuantity(), StockSource.LOGISTIC);
                productService.increaseQuantity(request.getReference(), request.getQuantity());

                return incomingRepository.save(existing);
        }

        @Transactional
        public void deleteIncoming(Long id) {
                IncomingMaterial existing = incomingRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Incoming record not found"));

                stockDep1Service.deductStock(existing.getReference(), existing.getLotNumber(), existing.getQuantity(),
                                StockSource.LOGISTIC);
                productService.increaseQuantity(existing.getReference(), -existing.getQuantity());

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
