package com.example.backend.modules.production.departement1.shared.service;

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

@Service
@RequiredArgsConstructor
public class OutgoingService {

    private final OutgoingRepository outgoingRepository;
    private final StockDep1Repository stockRepository;

    @Transactional
    public Outgoing registerOutgoing(OutgoingDTO request, User user) {
        // 1. Validate stock exists
        StockDep1 stock = stockRepository.findById(request.getStockId())
                .orElseThrow(() -> new EntityNotFoundException("Stock not found with ID: " + request.getStockId()));

        // 2. Validate quantity
        if (request.getQuantity() > stock.getQuantity()) {
            throw new IllegalArgumentException("Insufficient stock. Available: " + stock.getQuantity());
        }

        // 3. Create outgoing record
        Outgoing outgoing = new Outgoing();
        outgoing.setReference(request.getReference());
        outgoing.setQuantity(request.getQuantity());
        outgoing.setOutgoingDate(LocalDateTime.now());
        outgoing.setStock(stock);
        outgoing.setCreatedBy(user);

        Outgoing saved = outgoingRepository.save(outgoing);

        // 4. Update stock
        stock.setQuantity(stock.getQuantity() - request.getQuantity());
        stockRepository.save(stock);

        return saved;
    }

    @Transactional
    public Outgoing updateOutgoing(Long id, OutgoingDTO request) {
        Outgoing outgoing = outgoingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Outgoing material not found with ID: " + id));

        // Update fields
        if (request.getQuantity() != null) {
            // Recalculate stock change
            double quantityDiff = request.getQuantity() - outgoing.getQuantity();
            outgoing.setQuantity(request.getQuantity());

            // Update stock
            StockDep1 stock = outgoing.getStock();
            stock.setQuantity(stock.getQuantity() - quantityDiff);
            stockRepository.save(stock);
        }

        if (request.getReference() != null) {
            outgoing.setReference(request.getReference());
        }


        return outgoingRepository.save(outgoing);
    }

    @Transactional
    public void deleteOutgoing(Long id) {
        Outgoing outgoing = outgoingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Outgoing material not found with ID: " + id));

        // Restore stock
        StockDep1 stock = outgoing.getStock();
        stock.setQuantity(stock.getQuantity() + outgoing.getQuantity());
        stockRepository.save(stock);

        // Delete outgoing
        outgoingRepository.delete(outgoing);
    }

    public List<Outgoing> getAllOutgoing() {
        return outgoingRepository.findAll();
    }
}