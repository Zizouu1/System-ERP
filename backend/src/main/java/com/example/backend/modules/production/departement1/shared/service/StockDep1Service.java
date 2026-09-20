package com.example.backend.modules.production.departement1.shared.service;

import com.example.backend.modules.admin.product.entity.ProductTypeEnum;
import com.example.backend.modules.production.departement1.shared.entity.StockDep1;
import com.example.backend.modules.production.departement1.shared.repository.StockDep1Repository;
import com.example.backend.modules.production.productionstock.service.GlobalStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StockDep1Service {

    private final StockDep1Repository stockDep1Repository;
    private final GlobalStockService productService;

    public List<StockDep1> getAllStocks() {
        return stockDep1Repository.findAll();
    }

    @Transactional
    public void addStock(String reference, String lotNumber, ProductTypeEnum type, double quantity) {
        productService.validateRef(reference);
    StockDep1 stock = stockDep1Repository.findFirstByReferenceOrderByIdAsc(reference)
                .orElse(StockDep1.builder()
                        .reference(reference)
                        .productType(type)
                        .totalQuantity(0.0)
                        .storeQuantity(0.0)
                        .build());

        stock.setTotalQuantity(stock.getTotalQuantity() + quantity);
        stock.setStoreQuantity(stock.getStoreQuantity() + quantity);
        stockDep1Repository.save(stock);
    }

    @Transactional
    public void deductStock(String reference, String lotNumber, double quantity) {
        StockDep1 stock = findStock(reference, lotNumber);
        if (stock.getStoreQuantity() < quantity) {
            throw new IllegalArgumentException("Quantité en magasin insuffisante pour la référence: " + reference
                    + ". Disponible: " + stock.getStoreQuantity());
        }
        if (stock.getTotalQuantity() < quantity) {
            throw new IllegalArgumentException("Quantité totale insuffisante pour la référence: " + reference
                    + ". Disponible: " + stock.getTotalQuantity());
        }

        stock.setStoreQuantity(stock.getStoreQuantity() - quantity);
        stock.setTotalQuantity(stock.getTotalQuantity() - quantity);
        saveOrDeleteIfEmpty(stock);
    }

    @Transactional
    public void consumeStoreOnly(String reference, String lotNumber, double quantity) {
        StockDep1 stock = findStock(reference, lotNumber);
        if (stock.getStoreQuantity() < quantity) {
            throw new IllegalArgumentException("Quantité en magasin insuffisante pour la référence: " + reference
                    + ". Disponible: " + stock.getStoreQuantity());
        }
        stock.setStoreQuantity(stock.getStoreQuantity() - quantity);
        stockDep1Repository.save(stock);
    }

    @Transactional
    public void restoreStoreOnly(String reference, String lotNumber, double quantity) {
        StockDep1 stock = findStock(reference, lotNumber);
        double targetStore = stock.getStoreQuantity() + quantity;
        if (targetStore > stock.getTotalQuantity()) {
            throw new IllegalArgumentException(
                    "Restauration invalide: la quantité en magasin dépasserait la quantité totale.");
        }
        stock.setStoreQuantity(targetStore);
        stockDep1Repository.save(stock);
    }

    @Transactional
    public StockDep1 updateStock(Long id, StockDep1 updatedStock) {
        StockDep1 existing = stockDep1Repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock introuvable"));

        if (updatedStock.getStoreQuantity() > updatedStock.getTotalQuantity()) {
            throw new IllegalArgumentException("La quantité en magasin ne peut pas dépasser la quantité totale.");
        }
        if (updatedStock.getStoreQuantity() < 0 || updatedStock.getTotalQuantity() < 0) {
            throw new IllegalArgumentException("Les quantités ne peuvent pas être négatives.");
        }

        double diffTotal = updatedStock.getTotalQuantity() - existing.getTotalQuantity();
        if (diffTotal != 0) {
            productService.increaseQuantity(existing.getReference(), diffTotal, existing.getProductType());
        }

        existing.setTotalQuantity(updatedStock.getTotalQuantity());
        existing.setStoreQuantity(updatedStock.getStoreQuantity());

        return stockDep1Repository.save(existing);
    }

    @Transactional
    public void deleteStock(Long id) {
        StockDep1 existing = stockDep1Repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock introuvable"));
        productService.increaseQuantity(existing.getReference(), -existing.getTotalQuantity(), existing.getProductType());
        stockDep1Repository.delete(existing);
    }

    public Optional<StockDep1> findStockByReferenceAndLot(String reference, String lotNumber) {
        return stockDep1Repository.findFirstByReferenceOrderByIdAsc(reference);
    }

    private StockDep1 findStock(String reference, String lotNumber) {
        return stockDep1Repository.findFirstByReferenceOrderByIdAsc(reference)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Référence non trouvée dans le stock du Département 1: " + reference));
    }

    private void saveOrDeleteIfEmpty(StockDep1 stock) {
        if (stock.getTotalQuantity() <= 0.000001 && stock.getStoreQuantity() <= 0.000001) {
            stockDep1Repository.delete(stock);
            return;
        }
        stockDep1Repository.save(stock);
    }

}
