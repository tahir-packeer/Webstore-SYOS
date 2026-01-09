package org.example.business.managers;

import org.example.shared.dto.StockDTO;
import org.example.persistence.gateways.StockGateway;

import java.sql.SQLException;
import java.util.List;

public class StockManager {
    private static StockManager instance;
    private static final Object lock = new Object();
    private final StockGateway stockGateway;

    private StockManager() {
        this.stockGateway = StockGateway.getInstance();
    }

    public static StockManager getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new StockManager();
                }
            }
        }
        return instance;
    }

    public void addStock(StockDTO stock) throws SQLException, ClassNotFoundException {
        validateStock(stock);
        stockGateway.insert(stock);
        System.out.println("Stock added successfully for item: " + stock.getItemCode());
    }

    public void updateStock(StockDTO stock) throws SQLException, ClassNotFoundException {
        validateStock(stock);
        stockGateway.update(stock);
        System.out.println("Stock updated successfully for item: " + stock.getItemCode());
    }

    public List<StockDTO> getAllStock() throws SQLException, ClassNotFoundException {
        return stockGateway.findAll();
    }

    public List<StockDTO> getStockByItemId(int itemId) throws SQLException, ClassNotFoundException {
        return stockGateway.findByItemId(itemId);
    }

    public List<StockDTO> getLowStockItems(int threshold) throws SQLException, ClassNotFoundException {
        return stockGateway.findLowStock(threshold);
    }

    public int getTotalStockQuantity(int itemId) throws SQLException, ClassNotFoundException {
        return stockGateway.getTotalQuantityByItemId(itemId);
    }

    public boolean hasEnoughStock(int itemId, int requiredQuantity) throws SQLException, ClassNotFoundException {
        return stockGateway.hasEnoughStock(itemId, requiredQuantity);
    }

    public void reduceStock(int itemId, int quantity) throws SQLException, ClassNotFoundException {
        List<StockDTO> stockBatches = stockGateway.findByItemId(itemId);
        
        if (stockBatches.isEmpty()) {
            throw new IllegalStateException("No stock available for item ID: " + itemId);
        }

        int remainingToReduce = quantity;
        
        // Use FIFO - reduce from oldest stock first
        for (StockDTO stock : stockBatches) {
            if (remainingToReduce <= 0) break;
            
            int currentQuantity = stock.getQuantity();
            if (currentQuantity > 0) {
                int toReduce = Math.min(remainingToReduce, currentQuantity);
                int newQuantity = currentQuantity - toReduce;
                
                stockGateway.updateQuantity(stock.getId(), newQuantity);
                remainingToReduce -= toReduce;
                
                System.out.println("Reduced " + toReduce + " units from stock ID: " + stock.getId());
            }
        }

        if (remainingToReduce > 0) {
            throw new IllegalStateException("Insufficient stock. Could not reduce " + remainingToReduce + " units");
        }
    }

    private void validateStock(StockDTO stock) {
        if (stock == null) {
            throw new IllegalArgumentException("Stock cannot be null");
        }
        
        if (stock.getItemId() <= 0) {
            throw new IllegalArgumentException("Item ID must be valid");
        }
        
        if (stock.getQuantity() < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }
        
        if (stock.getDateOfPurchase() == null) {
            throw new IllegalArgumentException("Purchase date is required");
        }
        
        if (stock.getDateOfExpiry() == null) {
            throw new IllegalArgumentException("Expiry date is required");
        }
        
        if (stock.getDateOfExpiry().isBefore(stock.getDateOfPurchase())) {
            throw new IllegalArgumentException("Expiry date cannot be before purchase date");
        }
    }
}
