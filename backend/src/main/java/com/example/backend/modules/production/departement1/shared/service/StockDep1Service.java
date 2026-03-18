package com.example.backend.modules.production.departement1.shared.service;

import com.example.backend.modules.production.departement1.shared.entity.StockDep1;
import com.example.backend.modules.production.departement1.shared.entity.StockSource;
import com.example.backend.modules.production.departement1.shared.repository.StockDep1Repository;
import com.example.backend.modules.admin.product.entity.ProductTypeEnum;
import com.example.backend.modules.production.productionstock.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockDep1Service {

    private final StockDep1Repository stockDep1Repository;
    private final ProductService productService;

    public List<StockDep1> getAllStockBySource(StockSource source) {
        if (source == null) {
            return stockDep1Repository.findAll();
        }
        return stockDep1Repository.findBySource(source);
    }

    @Transactional
    public void addStock(String reference, String lotNumber, ProductTypeEnum type, double quantity, StockSource source) {
        productService.validateRef(reference);
        StockDep1 stock;

        // Find by reference and source (and lot if provided)
        if (lotNumber != null && !lotNumber.isEmpty()) {
            stock = stockDep1Repository.findByReferenceAndLotNumberAndSource(reference, lotNumber, source)
                    .orElse(StockDep1.builder()
                            .reference(reference)
                            .lotNumber(lotNumber)
                            .productType(type)
                            .source(source)
                            .totalQuantity(0.0)
                            .storeQuantity(0.0)
                            .build());
        } else {
            // Shared stock behavior: look for any entry with same ref and source
            stock = stockDep1Repository.findByReferenceAndSource(reference, source)
                    .stream().findFirst()
                    .orElse(StockDep1.builder()
                            .reference(reference)
                            .lotNumber(null)
                            .productType(type)
                            .source(source)
                            .totalQuantity(0.0)
                            .storeQuantity(0.0)
                            .build());
        }

        stock.setTotalQuantity(stock.getTotalQuantity() + quantity);
        stock.setStoreQuantity(stock.getStoreQuantity() + quantity);
        stockDep1Repository.save(stock);
    }

    @Transactional
    public void deductStock(String reference, String lotNumber, double quantity, StockSource source) {
        StockDep1 stock;
        if (lotNumber != null && !lotNumber.isEmpty()) {
            stock = stockDep1Repository.findByReferenceAndLotNumberAndSource(reference, lotNumber, source)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Référence non trouvée dans le stock du Département 1: " + reference + " (Lot: " + lotNumber
                                    + ")"));
        } else {
            stock = stockDep1Repository.findByReferenceAndSource(reference, source)
                    .stream().findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Référence non trouvée dans le stock du Département 1: " + reference));
        }

        if (stock.getStoreQuantity() < quantity) {
            throw new IllegalArgumentException("Quantité en magasin insuffisante pour la référence: " + reference
                    + ". Disponible: " + stock.getStoreQuantity());
        }

        stock.setStoreQuantity(stock.getStoreQuantity() - quantity);
        stockDep1Repository.save(stock);
    }

    @Transactional
    public StockDep1 updateStock(Long id, StockDep1 updatedStock) {
        StockDep1 existing = stockDep1Repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock introuvable"));

        double diffTotal = updatedStock.getTotalQuantity() - existing.getTotalQuantity();

        if (diffTotal != 0) {
            productService.increaseQuantity(existing.getReference(), diffTotal);
        }

        existing.setTotalQuantity(updatedStock.getTotalQuantity());
        existing.setStoreQuantity(updatedStock.getStoreQuantity());

        return stockDep1Repository.save(existing);
    }

    @Transactional
    public void deleteStock(Long id) {
        StockDep1 existing = stockDep1Repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock introuvable"));
        // Revert from main production stock
        productService.increaseQuantity(existing.getReference(), -existing.getTotalQuantity());
        stockDep1Repository.delete(existing);
    }
}
