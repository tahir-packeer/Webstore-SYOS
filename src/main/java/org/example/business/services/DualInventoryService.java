package org.example.business.services;

import org.example.persistence.gateways.ItemGateway;
import org.example.persistence.models.StockBatch;
import org.example.shared.dto.ItemDTO;
import org.example.persistence.database.DatabaseConnection;

import java.sql.*;
import java.util.*;

/**
 * SYOS Dual Inventory Management Service
 * Handles separate STORE and WEBSITE shelf inventory with batch-based stocking
 */
public class DualInventoryService {
    private static DualInventoryService instance;
    private static final Object lock = new Object();
    
    private final StockBatchService stockBatchService;
    private final ItemGateway itemGateway;
    private final DatabaseConnection dbConnection;
    
    public enum ShelfType {
        STORE, WEBSITE
    }
    
    public static class ShelfInventory {
        private int itemId;
        private String itemCode;
        private String itemName;
        private int storeQuantity;
        private int websiteQuantity;
        private int totalQuantity;
        private ShelfType lowStockShelf;
        
        // Getters and setters
        public int getItemId() { return itemId; }
        public void setItemId(int itemId) { this.itemId = itemId; }
        public String getItemCode() { return itemCode; }
        public void setItemCode(String itemCode) { this.itemCode = itemCode; }
        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }
        public int getStoreQuantity() { return storeQuantity; }
        public void setStoreQuantity(int storeQuantity) { this.storeQuantity = storeQuantity; }
        public int getWebsiteQuantity() { return websiteQuantity; }
        public void setWebsiteQuantity(int websiteQuantity) { this.websiteQuantity = websiteQuantity; }
        public int getTotalQuantity() { return totalQuantity; }
        public void setTotalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; }
        public ShelfType getLowStockShelf() { return lowStockShelf; }
        public void setLowStockShelf(ShelfType lowStockShelf) { this.lowStockShelf = lowStockShelf; }
    }
    
    private DualInventoryService() {
        this.stockBatchService = StockBatchService.getInstance();
        this.itemGateway = ItemGateway.getInstance();
        this.dbConnection = DatabaseConnection.getInstance();
    }
    
    public static DualInventoryService getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new DualInventoryService();
                }
            }
        }
        return instance;
    }
    
    /**
     * Stock items to shelf using FIFO batch selection
     */
    public boolean stockToShelf(String itemCode, int quantity, ShelfType shelfType) {
        try {
            // Get best batches using FIFO logic
            List<StockBatch> selectedBatches = stockBatchService.getAvailableBatchesForSale(itemCode, quantity);
            
            int totalAvailable = selectedBatches.stream()
                    .mapToInt(StockBatch::getCurrentQuantity)
                    .sum();
            
            if (totalAvailable < quantity) {
                throw new IllegalArgumentException("Insufficient stock in warehouse. Available: " + totalAvailable + ", Required: " + quantity);
            }
            
            Connection connection = dbConnection.connect();
            try {
                connection.setAutoCommit(false);
                
                ItemDTO itemDTO = itemGateway.findByCode(itemCode);
                if (itemDTO == null) {
                    throw new IllegalArgumentException("Item not found: " + itemCode);
                }
                
                // Update shelf inventory
                String updateShelfQuery = """
                    INSERT INTO shelf (item_id, quantity, type) VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE quantity = quantity + ?
                """;
                PreparedStatement shelfStmt = connection.prepareStatement(updateShelfQuery);
                shelfStmt.setInt(1, itemDTO.getId());
                shelfStmt.setInt(2, quantity);
                shelfStmt.setString(3, shelfType.name());
                shelfStmt.setInt(4, quantity);
                shelfStmt.executeUpdate();
                
                // Record shelf stock movements for tracking
                for (StockBatch batch : selectedBatches) {
                    int batchQuantityUsed = batch.getCurrentQuantity();
                    
                    // Get shelf ID
                    String getShelfIdQuery = "SELECT id FROM shelf WHERE item_id = ? AND type = ?";
                    PreparedStatement getShelfStmt = connection.prepareStatement(getShelfIdQuery);
                    getShelfStmt.setInt(1, itemDTO.getId());
                    getShelfStmt.setString(2, shelfType.name());
                    ResultSet shelfRs = getShelfStmt.executeQuery();
                    
                    if (shelfRs.next()) {
                        int shelfId = shelfRs.getInt("id");
                        
                        // Record movement in shelf_stock tracking table
                        String trackingQuery = "INSERT INTO shelf_stock (stock_id, shelf_id, quantity_moved) VALUES (?, ?, ?)";
                        PreparedStatement trackStmt = connection.prepareStatement(trackingQuery);
                        trackStmt.setInt(1, batch.getId());
                        trackStmt.setInt(2, shelfId);
                        trackStmt.setInt(3, batchQuantityUsed);
                        trackStmt.executeUpdate();
                        trackStmt.close();
                    }
                    getShelfStmt.close();
                }
                
                // Reduce stock quantities using existing service
                boolean stockReduced = stockBatchService.processSale(itemCode, quantity);
                if (!stockReduced) {
                    connection.rollback();
                    return false;
                }
                
                connection.commit();
                shelfStmt.close();
                return true;
                
            } catch (Exception e) {
                connection.rollback();
                return false;
            } finally {
                connection.setAutoCommit(true);
                dbConnection.closeConnection(connection);
            }
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Transfer items between shelves
     */
    public boolean transferBetweenShelves(String itemCode, int quantity, ShelfType fromShelf, ShelfType toShelf) {
        try {
            Connection connection = dbConnection.connect();
            try {
                connection.setAutoCommit(false);
                
                ItemDTO itemDTO = itemGateway.findByCode(itemCode);
                if (itemDTO == null) {
                    throw new IllegalArgumentException("Item not found: " + itemCode);
                }
                
                // Check if source shelf has enough quantity
                String checkQuantityQuery = "SELECT quantity FROM shelf WHERE item_id = ? AND type = ?";
                PreparedStatement checkStmt = connection.prepareStatement(checkQuantityQuery);
                checkStmt.setInt(1, itemDTO.getId());
                checkStmt.setString(2, fromShelf.name());
                ResultSet rs = checkStmt.executeQuery();
                
                int availableQuantity = 0;
                if (rs.next()) {
                    availableQuantity = rs.getInt("quantity");
                }
                
                if (availableQuantity < quantity) {
                    throw new IllegalArgumentException("Insufficient quantity in " + fromShelf + " shelf. Available: " + availableQuantity + ", Required: " + quantity);
                }
                
                // Reduce from source shelf
                String reduceQuery = "UPDATE shelf SET quantity = quantity - ? WHERE item_id = ? AND type = ?";
                PreparedStatement reduceStmt = connection.prepareStatement(reduceQuery);
                reduceStmt.setInt(1, quantity);
                reduceStmt.setInt(2, itemDTO.getId());
                reduceStmt.setString(3, fromShelf.name());
                reduceStmt.executeUpdate();
                
                // Add to destination shelf
                String addQuery = """
                    INSERT INTO shelf (item_id, quantity, type) VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE quantity = quantity + ?
                """;
                PreparedStatement addStmt = connection.prepareStatement(addQuery);
                addStmt.setInt(1, itemDTO.getId());
                addStmt.setInt(2, quantity);
                addStmt.setString(3, toShelf.name());
                addStmt.setInt(4, quantity);
                addStmt.executeUpdate();
                
                connection.commit();
                checkStmt.close();
                reduceStmt.close();
                addStmt.close();
                return true;
                
            } catch (Exception e) {
                connection.rollback();
                return false;
            } finally {
                connection.setAutoCommit(true);
                dbConnection.closeConnection(connection);
            }
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check availability in specific shelf
     */
    public int getShelfQuantity(String itemCode, ShelfType shelfType) {
        try {
            ItemDTO itemDTO = itemGateway.findByCode(itemCode);
            if (itemDTO == null) {
                return 0;
            }
            
            Connection connection = dbConnection.connect();
            String query = "SELECT quantity FROM shelf WHERE item_id = ? AND type = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, itemDTO.getId());
            stmt.setString(2, shelfType.name());
            ResultSet rs = stmt.executeQuery();
            
            int quantity = 0;
            if (rs.next()) {
                quantity = rs.getInt("quantity");
            }
            
            stmt.close();
            dbConnection.closeConnection(connection);
            return quantity;
            
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Process sale from specific shelf (for counter vs online sales)
     */
    public boolean processSaleFromShelf(String itemCode, int quantity, ShelfType shelfType) {
        try {
            int availableQuantity = getShelfQuantity(itemCode, shelfType);
            if (availableQuantity < quantity) {
                return false;
            }
            
            ItemDTO itemDTO = itemGateway.findByCode(itemCode);
            if (itemDTO == null) {
                return false;
            }
            
            Connection connection = dbConnection.connect();
            String updateQuery = "UPDATE shelf SET quantity = quantity - ? WHERE item_id = ? AND type = ?";
            PreparedStatement stmt = connection.prepareStatement(updateQuery);
            stmt.setInt(1, quantity);
            stmt.setInt(2, itemDTO.getId());
            stmt.setString(3, shelfType.name());
            
            int rowsUpdated = stmt.executeUpdate();
            
            stmt.close();
            dbConnection.closeConnection(connection);
            return rowsUpdated > 0;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get comprehensive dual inventory overview
     */
    public List<ShelfInventory> getDualInventoryOverview() {
        try {
            Connection connection = dbConnection.connect();
            String query = """
                SELECT 
                    i.id as item_id,
                    i.code as item_code,
                    i.name as item_name,
                    COALESCE(store_shelf.quantity, 0) as store_quantity,
                    COALESCE(website_shelf.quantity, 0) as website_quantity
                FROM items i
                LEFT JOIN shelf store_shelf ON i.id = store_shelf.item_id AND store_shelf.type = 'STORE'
                LEFT JOIN shelf website_shelf ON i.id = website_shelf.item_id AND website_shelf.type = 'WEBSITE'
                ORDER BY i.name
            """;
            
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            List<ShelfInventory> inventoryList = new ArrayList<>();
            
            while (rs.next()) {
                ShelfInventory inventory = new ShelfInventory();
                inventory.setItemId(rs.getInt("item_id"));
                inventory.setItemCode(rs.getString("item_code"));
                inventory.setItemName(rs.getString("item_name"));
                inventory.setStoreQuantity(rs.getInt("store_quantity"));
                inventory.setWebsiteQuantity(rs.getInt("website_quantity"));
                inventory.setTotalQuantity(inventory.getStoreQuantity() + inventory.getWebsiteQuantity());
                
                // Determine which shelf has low stock
                if (inventory.getStoreQuantity() < 5 && inventory.getWebsiteQuantity() >= 5) {
                    inventory.setLowStockShelf(ShelfType.STORE);
                } else if (inventory.getWebsiteQuantity() < 5 && inventory.getStoreQuantity() >= 5) {
                    inventory.setLowStockShelf(ShelfType.WEBSITE);
                }
                
                inventoryList.add(inventory);
            }
            
            stmt.close();
            dbConnection.closeConnection(connection);
            return inventoryList;
            
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * Get low stock alerts for specific shelf type
     */
    public List<ShelfInventory> getLowStockAlerts(ShelfType shelfType, int minimumQuantity) {
        try {
            Connection connection = dbConnection.connect();
            String query = """
                SELECT 
                    i.id as item_id,
                    i.code as item_code,
                    i.name as item_name,
                    s.quantity
                FROM items i
                JOIN shelf s ON i.id = s.item_id
                WHERE s.type = ? AND s.quantity <= ?
                ORDER BY s.quantity ASC
            """;
            
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, shelfType.name());
            stmt.setInt(2, minimumQuantity);
            ResultSet rs = stmt.executeQuery();
            
            List<ShelfInventory> lowStockItems = new ArrayList<>();
            
            while (rs.next()) {
                ShelfInventory inventory = new ShelfInventory();
                inventory.setItemId(rs.getInt("item_id"));
                inventory.setItemCode(rs.getString("item_code"));
                inventory.setItemName(rs.getString("item_name"));
                
                if (shelfType == ShelfType.STORE) {
                    inventory.setStoreQuantity(rs.getInt("quantity"));
                    inventory.setWebsiteQuantity(0); // Will be filled if needed
                } else {
                    inventory.setWebsiteQuantity(rs.getInt("quantity"));
                    inventory.setStoreQuantity(0); // Will be filled if needed
                }
                
                inventory.setLowStockShelf(shelfType);
                lowStockItems.add(inventory);
            }
            
            stmt.close();
            dbConnection.closeConnection(connection);
            return lowStockItems;
            
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * Check if item is available for sale on specific shelf
     */
    public boolean isAvailableOnShelf(String itemCode, int quantity, ShelfType shelfType) {
        return getShelfQuantity(itemCode, shelfType) >= quantity;
    }
    
    /**
     * Get shelf stock movement history
     */
    public List<Map<String, Object>> getShelfMovementHistory(String itemCode, int limit) {
        try {
            ItemDTO itemDTO = itemGateway.findByCode(itemCode);
            if (itemDTO == null) {
                return new ArrayList<>();
            }
            
            Connection connection = dbConnection.connect();
            String query = """
                SELECT 
                    ss.quantity_moved,
                    ss.move_date,
                    s.type as shelf_type,
                    st.date_of_purchase,
                    st.date_of_expiry
                FROM shelf_stock ss
                JOIN shelf s ON ss.shelf_id = s.id
                JOIN stock st ON ss.stock_id = st.id
                WHERE s.item_id = ?
                ORDER BY ss.move_date DESC
                LIMIT ?
            """;
            
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, itemDTO.getId());
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            
            List<Map<String, Object>> movements = new ArrayList<>();
            
            while (rs.next()) {
                Map<String, Object> movement = new HashMap<>();
                movement.put("quantityMoved", rs.getInt("quantity_moved"));
                movement.put("moveDate", rs.getTimestamp("move_date"));
                movement.put("shelfType", rs.getString("shelf_type"));
                movement.put("batchPurchaseDate", rs.getDate("date_of_purchase"));
                movement.put("batchExpiryDate", rs.getDate("date_of_expiry"));
                movements.add(movement);
            }
            
            stmt.close();
            dbConnection.closeConnection(connection);
            return movements;
            
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
