package com.example.backend.modules.production.departement1.shared.service;

import com.example.backend.modules.admin.product.service.ProductService;
import com.example.backend.modules.admin.usermanagement.entity.User;
import com.example.backend.modules.production.departement1.shared.dto.OutgoingDTO;
import com.example.backend.modules.production.departement1.shared.entity.Outgoing;
import com.example.backend.modules.production.departement1.shared.entity.StockDep1;
import com.example.backend.modules.production.departement1.shared.repository.OutgoingRepository;
import com.example.backend.modules.production.departement1.shared.repository.StockDep1Repository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OutgoingService {

    private final OutgoingRepository outgoingRepository;
    private final StockDep1Repository stockRepository;
    private final ProductService masterProductService;

    @Transactional
    public Outgoing registerOutgoing(OutgoingDTO request, User user) {
        // Validate product reference exists in Product master
        masterProductService.validateProductReference(request.getReference());
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
                .build();

        Outgoing saved = outgoingRepository.save(outgoing);

        stock.setStoreQuantity(stock.getStoreQuantity() - quantity);
        stockRepository.save(stock);

        return saved;
    }

    @Transactional
    public Outgoing updateOutgoing(Long id, OutgoingDTO request) {
        Outgoing outgoing = outgoingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Outgoing material not found with ID: " + id));

        String reference = request.getReference() != null ? request.getReference() : outgoing.getReference();
        String lotNumber = request.getLotNumber() != null ? request.getLotNumber() : outgoing.getLotNumber();
        double requestedQuantity = request.getQuantityOut() != null ? request.getQuantityOut()
                : outgoing.getQuantityOut();
        if (requestedQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        StockDep1 oldStock = findMatchingStock(outgoing.getReference(), outgoing.getLotNumber());
        oldStock.setStoreQuantity(oldStock.getStoreQuantity() + outgoing.getQuantityOut());
        stockRepository.save(oldStock);

        StockDep1 newStock = findMatchingStock(reference, lotNumber);
        if (newStock.getStoreQuantity() < requestedQuantity) {
            throw new IllegalArgumentException("Insufficient stock. Available: " + newStock.getStoreQuantity());
        }

        newStock.setStoreQuantity(newStock.getStoreQuantity() - requestedQuantity);
        stockRepository.save(newStock);

        outgoing.setReference(reference);
        outgoing.setLotNumber(lotNumber);
        outgoing.setQuantityOut(requestedQuantity);
        if (request.getNotes() != null) {
            outgoing.setNotes(request.getNotes());
        }
        if (request.getOperationDate() != null) {
            outgoing.setOperationDate(request.getOperationDate());
        }


        return outgoingRepository.save(outgoing);
    }

    @Transactional
    public void deleteOutgoing(Long id) {
        Outgoing outgoing = outgoingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Outgoing material not found with ID: " + id));

        StockDep1 stock = findMatchingStock(outgoing.getReference(), outgoing.getLotNumber());
        stock.setStoreQuantity(stock.getStoreQuantity() + outgoing.getQuantityOut());
        stockRepository.save(stock);

        outgoingRepository.delete(outgoing);
    }

    public List<Outgoing> getAllOutgoing() {
        return outgoingRepository.findAll();
    }

    private double requirePositiveQuantity(Double quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        return quantity;
    }

    private StockDep1 findMatchingStock(String reference, String lotNumber) {
        return stockRepository.findAll().stream()
                .filter(stock -> Objects.equals(stock.getReference(), reference)
                        && Objects.equals(stock.getLotNumber(), lotNumber))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Stock not found for reference " + reference + " and lot " + lotNumber));
    }
}