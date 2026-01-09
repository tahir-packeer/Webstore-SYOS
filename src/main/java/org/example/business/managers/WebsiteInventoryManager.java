package org.example.business.managers;

import org.example.persistence.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class WebsiteInventoryManager {
    private static WebsiteInventoryManager instance;
    private static final Object lock = new Object();
    private final DatabaseConnection dbConnection;

    private WebsiteInventoryManager() {
        this.dbConnection = DatabaseConnection.getInstance();
        initializeWebsiteInventory();
    }

    public static WebsiteInventoryManager getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new WebsiteInventoryManager();
                }
            }
        }
        return instance;
    }

    // Initialize website inventory with default values from store stock
    private void initializeWebsiteInventory() {
        try {
            Connection connection = dbConnection.connect();
            
            // Check if website inventory is already initialized
            String checkQuery = "SELECT COUNT(*) as count FROM shelf WHERE type = 'WEBSITE'";
            try (PreparedStatement checkStatement = connection.prepareStatement(checkQuery);
                 ResultSet checkResult = checkStatement.executeQuery()) {
                
                if (checkResult.next() && checkResult.getInt("count") > 0) {
                    System.out.println("Website inventory already initialized");
                    return;
                }
            }
            
            // Initialize with 50% of store stock
            String storeStockQuery = """
                SELECT i.id, COALESCE(SUM(s.quantity), 0) as store_stock
                FROM items i 
                LEFT JOIN stock s ON i.id = s.item_id AND s.availability = true
                GROUP BY i.id
            """;
            
            try (PreparedStatement storeStatement = connection.prepareStatement(storeStockQuery);
                 ResultSet storeResult = storeStatement.executeQuery()) {
                
                String insertQuery = """
                    INSERT INTO shelf (item_id, quantity, type) 
                    VALUES (?, ?, 'WEBSITE') 
                    ON DUPLICATE KEY UPDATE quantity = VALUES(quantity)
                """;
                
                try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
                    while (storeResult.next()) {
                        int itemId = storeResult.getInt("id");
                        int storeStock = storeResult.getInt("store_stock");
                        int websiteStock = Math.max(0, storeStock / 2);
                        
                        insertStatement.setInt(1, itemId);
                        insertStatement.setInt(2, websiteStock);
                        insertStatement.addBatch();
                    }
                    insertStatement.executeBatch();
                    System.out.println("Website inventory initialized successfully");
                }
            } finally {
                dbConnection.closeConnection(connection);
            }
        } catch (Exception e) {
            System.err.println("Error initializing website inventory: " + e.getMessage());
        }
    }

    // Check if enough stock is available on website
    public boolean hasEnoughStock(int itemId, int quantity) {
        try {
            Connection connection = dbConnection.connect();
            String query = "SELECT quantity FROM shelf WHERE item_id = ? AND type = 'WEBSITE'";
            
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, itemId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        int currentStock = resultSet.getInt("quantity");
                        return currentStock >= quantity;
                    }
                    return false; // Item not found in website inventory
                }
            } finally {
                dbConnection.closeConnection(connection);
            }
        } catch (Exception e) {
            System.err.println("Error checking website stock: " + e.getMessage());
            return false;
        }
    }

    // Reduce website stock after purchase
    public boolean reduceStock(int itemId, int quantity) {
        try {
            Connection connection = dbConnection.connect();
            
            // First check current stock
            String checkQuery = "SELECT quantity FROM shelf WHERE item_id = ? AND type = 'WEBSITE'";
            try (PreparedStatement checkStatement = connection.prepareStatement(checkQuery)) {
                checkStatement.setInt(1, itemId);
                try (ResultSet resultSet = checkStatement.executeQuery()) {
                    if (resultSet.next()) {
                        int currentStock = resultSet.getInt("quantity");
                        if (currentStock >= quantity) {
                            // Update stock
                            String updateQuery = "UPDATE shelf SET quantity = quantity - ? WHERE item_id = ? AND type = 'WEBSITE'";
                            try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                                updateStatement.setInt(1, quantity);
                                updateStatement.setInt(2, itemId);
                                int rowsAffected = updateStatement.executeUpdate();
                                
                                if (rowsAffected > 0) {
                                    System.out.println("Website inventory reduced for item " + itemId + ": " + quantity + " units");
                                    return true;
                                }
                            }
                        } else {
                            System.err.println("Insufficient website stock for item " + itemId + ": requested " + quantity + ", available " + currentStock);
                            return false;
                        }
                    } else {
                        System.err.println("Item " + itemId + " not found in website inventory");
                        return false;
                    }
                }
            } finally {
                dbConnection.closeConnection(connection);
            }
        } catch (Exception e) {
            System.err.println("Error reducing website stock: " + e.getMessage());
        }
        return false;
    }

    // Add stock to website inventory (when restocking from store)
    public void addStock(int itemId, int quantity) {
        try {
            Connection connection = dbConnection.connect();
            String query = """
                INSERT INTO shelf (item_id, quantity, type) 
                VALUES (?, ?, 'WEBSITE') 
                ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity)
            """;
            
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, itemId);
                statement.setInt(2, quantity);
                int rowsAffected = statement.executeUpdate();
                
                if (rowsAffected > 0) {
                    System.out.println("Website inventory increased for item " + itemId + ": " + quantity + " units");
                }
            } finally {
                dbConnection.closeConnection(connection);
            }
        } catch (Exception e) {
            System.err.println("Error adding website stock: " + e.getMessage());
        }
    }

    // Get current website stock for an item
    public int getWebsiteStock(int itemId) {
        try {
            Connection connection = dbConnection.connect();
            String query = "SELECT quantity FROM shelf WHERE item_id = ? AND type = 'WEBSITE'";
            
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, itemId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt("quantity");
                    }
                    return 0; // Item not found
                }
            } finally {
                dbConnection.closeConnection(connection);
            }
        } catch (Exception e) {
            System.err.println("Error getting website stock: " + e.getMessage());
            return 0;
        }
    }

    // Get all website inventory
    public Map<Integer, Integer> getAllWebsiteInventory() {
        Map<Integer, Integer> inventory = new HashMap<>();
        try {
            Connection connection = dbConnection.connect();
            String query = "SELECT item_id, quantity FROM shelf WHERE type = 'WEBSITE'";
            
            try (PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet resultSet = statement.executeQuery()) {
                
                while (resultSet.next()) {
                    int itemId = resultSet.getInt("item_id");
                    int quantity = resultSet.getInt("quantity");
                    inventory.put(itemId, quantity);
                }
            } finally {
                dbConnection.closeConnection(connection);
            }
        } catch (Exception e) {
            System.err.println("Error getting all website inventory: " + e.getMessage());
        }
        return inventory;
    }

    // Transfer stock from store to website
    public boolean transferFromStoreToWebsite(int itemId, int quantity) throws SQLException, ClassNotFoundException {
        // In production, this would involve both store and website inventory tables
        StockManager storeManager = StockManager.getInstance();
        
        if (storeManager.hasEnoughStock(itemId, quantity)) {
            // Reduce from store and add to website
            storeManager.reduceStock(itemId, quantity);
            addStock(itemId, quantity);
            System.out.println("Transferred " + quantity + " units of item " + itemId + " from store to website");
            return true;
        }
        
        System.err.println("Failed to transfer stock from store to website for item " + itemId);
        return false;
    }
}
