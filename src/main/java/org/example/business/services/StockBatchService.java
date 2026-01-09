package org.example.business.services;

import org.example.persistence.gateways.StockGateway;
import org.example.persistence.gateways.ItemGateway;
import org.example.persistence.models.StockBatch;
import org.example.persistence.models.Item;
import org.example.shared.dto.StockDTO;
import org.example.shared.dto.ItemDTO;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;


public class StockBatchService {
    private static StockBatchService instance;
    private static final Object lock = new Object();
    
    private final StockGateway stockGateway;
    private final ItemGateway itemGateway;
    
    private StockBatchService() {
        this.stockGateway = StockGateway.getInstance();
        this.itemGateway = ItemGateway.getInstance();
    }
    
    public static StockBatchService getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new StockBatchService();
                }
            }
        }
        return instance;
    }
    
    public boolean addStockBatch(String itemCode, int quantity, Date expiryDate, 
                                double purchasePrice, String supplierInfo) {
        try {
            // Get item by code
            ItemDTO itemDTO = itemGateway.findByCode(itemCode);
            if (itemDTO == null) {
                throw new IllegalArgumentException("Item not found with code: " + itemCode);
            }
            
            // Create stock DTO
            StockDTO stockDTO = new StockDTO();
            stockDTO.setItemId(itemDTO.getId());
            stockDTO.setItemCode(itemDTO.getCode());
            stockDTO.setItemName(itemDTO.getName());
            stockDTO.setQuantity(quantity);
            stockDTO.setDateOfPurchase(LocalDate.now());
            stockDTO.setDateOfExpiry(convertToLocalDate(expiryDate));
            stockDTO.setAvailability(true);
            
            stockGateway.insert(stockDTO);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get items for sale using FIFO and expiry-based selection
     * Automatically selects best batches based on expiry dates
     */
    public List<StockBatch> getAvailableBatchesForSale(String itemCode, int requiredQuantity) {
        try {
            ItemDTO itemDTO = itemGateway.findByCode(itemCode);
            if (itemDTO == null) {
                return new ArrayList<>();
            }
            
            List<StockDTO> stockDTOs = stockGateway.getAllByItemId(itemDTO.getId());
            List<StockBatch> batches = stockDTOs.stream()
                    .map(this::convertToStockBatch)
                    .filter(batch -> batch.isAvailable() && !batch.isExpired())
                    .collect(Collectors.toList());
            
            // Sort by FIFO strategy: earliest expiry first, then earliest purchase
            batches.sort((a, b) -> {
                // First priority: near expiry items (within 7 days)
                boolean aIsNearExpiry = a.isNearExpiry();
                boolean bIsNearExpiry = b.isNearExpiry();
                
                if (aIsNearExpiry && !bIsNearExpiry) return -1;
                if (!aIsNearExpiry && bIsNearExpiry) return 1;
                
                // Second priority: expiry date (earliest first)
                int expiryComparison = a.getExpiryDate().compareTo(b.getExpiryDate());
                if (expiryComparison != 0) return expiryComparison;
                
                // Third priority: purchase date (earliest first)
                return a.getPurchaseDate().compareTo(b.getPurchaseDate());
            });
            
            // Select batches to fulfill the required quantity
            List<StockBatch> selectedBatches = new ArrayList<>();
            int remainingQuantity = requiredQuantity;
            
            for (StockBatch batch : batches) {
                if (remainingQuantity <= 0) break;
                
                int availableInBatch = Math.min(batch.getCurrentQuantity(), remainingQuantity);
                if (availableInBatch > 0) {
                    StockBatch selectedBatch = new StockBatch();
                    selectedBatch.setId(batch.getId());
                    selectedBatch.setItem(batch.getItem());
                    selectedBatch.setCurrentQuantity(availableInBatch);
                    selectedBatch.setExpiryDate(batch.getExpiryDate());
                    selectedBatch.setPurchaseDate(batch.getPurchaseDate());
                    
                    selectedBatches.add(selectedBatch);
                    remainingQuantity -= availableInBatch;
                }
            }
            
            return selectedBatches;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * Process sale and reduce stock quantities
     */
    public boolean processSale(String itemCode, int quantity) {
        try {
            List<StockBatch> selectedBatches = getAvailableBatchesForSale(itemCode, quantity);
            
            int totalAvailable = selectedBatches.stream()
                    .mapToInt(StockBatch::getCurrentQuantity)
                    .sum();
            
            if (totalAvailable < quantity) {
                throw new IllegalArgumentException("Insufficient stock. Available: " + totalAvailable + ", Required: " + quantity);
            }
            
            // Update stock quantities in database
            for (StockBatch batch : selectedBatches) {
                StockDTO stockDTO = stockGateway.getById(batch.getId());
                int newQuantity = stockDTO.getQuantity() - batch.getCurrentQuantity();
                stockDTO.setQuantity(newQuantity);
                stockDTO.setAvailability(newQuantity > 0);
                
                stockGateway.update(stockDTO);
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get expiry alerts for items expiring within specified days
     */
    public List<StockBatch> getExpiryAlerts(int daysAhead) {
        try {
            List<StockDTO> allStock = stockGateway.getAllStock();
            LocalDate cutoffDate = LocalDate.now().plusDays(daysAhead);
            
            return allStock.stream()
                    .map(this::convertToStockBatch)
                    .filter(batch -> batch.isAvailable())
                    .filter(batch -> convertToLocalDate(batch.getExpiryDate()).isBefore(cutoffDate) || 
                                   convertToLocalDate(batch.getExpiryDate()).equals(cutoffDate))
                    .sorted(Comparator.comparing(StockBatch::getExpiryDate))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * Get low stock alerts for items with less than minimum quantity
     */
    public List<StockBatch> getLowStockAlerts(int minimumQuantity) {
        try {
            List<StockDTO> allStock = stockGateway.getAllStock();
            Map<Integer, Integer> itemTotalQuantities = new HashMap<>();
            Map<Integer, ItemDTO> itemDetails = new HashMap<>();
            
            // Calculate total quantities per item
            for (StockDTO stock : allStock) {
                if (stock.isAvailability()) {
                    itemTotalQuantities.merge(stock.getItemId(), stock.getQuantity(), Integer::sum);
                    itemDetails.put(stock.getItemId(), 
                        new ItemDTO(stock.getItemId(), stock.getItemCode(), stock.getItemName(), 0.0));
                }
            }
            
            List<StockBatch> lowStockItems = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : itemTotalQuantities.entrySet()) {
                if (entry.getValue() < minimumQuantity) {
                    ItemDTO itemDTO = itemDetails.get(entry.getKey());
                    Item item = convertToItem(itemDTO);
                    StockBatch batch = new StockBatch();
                    batch.setItem(item);
                    batch.setCurrentQuantity(entry.getValue());
                    lowStockItems.add(batch);
                }
            }
            
            return lowStockItems;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * Get comprehensive stock report for an item
     */
    public Map<String, Object> getItemStockReport(String itemCode) {
        try {
            ItemDTO itemDTO = itemGateway.findByCode(itemCode);
            if (itemDTO == null) {
                return null;
            }
            
            List<StockDTO> itemStock = stockGateway.getAllByItemId(itemDTO.getId());
            List<StockBatch> batches = itemStock.stream()
                    .map(this::convertToStockBatch)
                    .collect(Collectors.toList());
            
            Map<String, Object> report = new HashMap<>();
            report.put("item", convertToItem(itemDTO));
            report.put("totalBatches", batches.size());
            report.put("totalQuantity", batches.stream().mapToInt(StockBatch::getCurrentQuantity).sum());
            report.put("availableQuantity", batches.stream()
                    .filter(StockBatch::isAvailable)
                    .mapToInt(StockBatch::getCurrentQuantity).sum());
            report.put("expiredBatches", batches.stream()
                    .filter(StockBatch::isExpired)
                    .collect(Collectors.toList()));
            report.put("nearExpiryBatches", batches.stream()
                    .filter(StockBatch::isNearExpiry)
                    .collect(Collectors.toList()));
            report.put("batches", batches);
            
            return report;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Check stock availability for multiple items (for POS system)
     */
    public Map<String, Integer> checkStockAvailability(Map<String, Integer> requiredItems) {
        Map<String, Integer> availability = new HashMap<>();
        
        for (Map.Entry<String, Integer> entry : requiredItems.entrySet()) {
            String itemCode = entry.getKey();
            int requiredQuantity = entry.getValue();
            
            List<StockBatch> availableBatches = getAvailableBatchesForSale(itemCode, requiredQuantity);
            int totalAvailable = availableBatches.stream()
                    .mapToInt(StockBatch::getCurrentQuantity)
                    .sum();
            
            availability.put(itemCode, totalAvailable);
        }
        
        return availability;
    }
    
    // Helper Methods - Convert between DTOs and Models
    private StockBatch convertToStockBatch(StockDTO dto) {
        try {
            StockBatch batch = new StockBatch();
            batch.setId(dto.getId());
            
            // Create Item from DTO data
            Item item = new Item(dto.getItemCode(), dto.getItemName(), 0.0); // Price will be fetched separately if needed
            item.setId(dto.getItemId());
            batch.setItem(item);
            
            batch.setCurrentQuantity(dto.getQuantity());
            batch.setOriginalQuantity(dto.getQuantity()); // Assuming current is original for existing records
            batch.setExpiryDate(convertToDate(dto.getDateOfExpiry()));
            batch.setPurchaseDate(convertToDate(dto.getDateOfPurchase()));
            batch.setAvailable(dto.isAvailability());
            return batch;
        } catch (Exception e) {
            return null;
        }
    }
    
    private Item convertToItem(ItemDTO dto) {
        Item item = new Item(dto.getCode(), dto.getName(), dto.getPrice());
        item.setId(dto.getId());
        return item;
    }
    
    private Date convertToDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
    
    private LocalDate convertToLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
