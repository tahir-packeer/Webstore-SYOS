package org.example.presentation.controllers;

import org.example.persistence.database.DatabaseConnection;
import org.example.persistence.models.Item;
import org.example.persistence.models.Stock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StockController {
    public java.util.List<Stock> getAllStocks() throws SQLException, ClassNotFoundException {
        java.util.List<Stock> stocks = new java.util.ArrayList<>();
        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();
        String query = "SELECT * FROM stock ORDER BY id DESC LIMIT 100";
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Item item = new Item("", "", 0);
                item.setId(rs.getInt("item_id"));
                Stock stock = new Stock(item,
                    rs.getInt("quantity"),
                    rs.getDate("date_of_expiry")
                );
                stock.setId(rs.getInt("id"));
                stock.setDate_of_purchase(rs.getDate("date_of_purchase"));
                stock.setAvailability(rs.getBoolean("availability"));
                stocks.add(stock);
            }
        }
        return stocks;
    }

    public void add_items_to_stock
            (Stock stock)
            throws SQLException, ClassNotFoundException
    {
        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();
        PreparedStatement statement = null;

        try {
            statement = connection.prepareStatement("insert into stock(" +
                    "item_id, quantity, date_of_purchase, date_of_expiry, availability" +
                    ") values(?,?,?,?,?)");

            statement.setInt(1, stock.getItem().getId());
            statement.setInt(2, stock.getQuantity());
            statement.setDate(3, new java.sql.Date(stock.getDate_of_purchase().getTime()));
            statement.setDate(4, new java.sql.Date(stock.getDate_of_expiry().getTime()));
            statement.setBoolean(5, stock.isAvailability());

            statement.executeUpdate();
        } finally {
            if (statement != null) statement.close();
            db.closeConnection(connection);
        }
    }

    public int get_Stock_quantity_by_item
            (Item item)
            throws SQLException, ClassNotFoundException
    {
        if (item == null) {
            return 0;
        }
        
        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();
        PreparedStatement statement = null;

        try {
            statement = connection.prepareStatement("select sum(quantity) from stock where item_id = ?");
            statement.setInt(1, item.getId());

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                int totalQuantity = resultSet.getInt(1);
                return totalQuantity;
            } else {
                return 0;
            }
        } finally {
            if (statement != null) statement.close();
            db.closeConnection(connection);
        }
    }

    public void reduce_stock_quantity_and_update_stock_shelf_table
            (Item item, int quantity, int shelf_id)
            throws SQLException, ClassNotFoundException
    {

        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();

        try{
            connection.setAutoCommit(false);

            int remainingQuantity = quantity;

            // Modified query to prioritize by expiry date first, then by purchase date (FIFO)
            // If expiry date is closer than oldest batch, choose the newer batch
            PreparedStatement getStockQuery = connection.prepareStatement(
                    "SELECT id, quantity, date_of_expiry, date_of_purchase FROM stock " +
                    "WHERE item_id = ? AND quantity > 0 " +
                    "ORDER BY date_of_expiry ASC, date_of_purchase ASC"
            );
            getStockQuery.setInt(1, item.getId());
            ResultSet stockOfItem = getStockQuery.executeQuery();

            while (stockOfItem.next()) {
                int stockId = stockOfItem.getInt("id");
                int stockQuantity = stockOfItem.getInt("quantity");

                int quantityToReduce = Math.min(remainingQuantity, stockQuantity);
                PreparedStatement updateStockQuery = connection.prepareStatement(
                        "UPDATE stock SET quantity = quantity - ? WHERE id = ?"
                );
                updateStockQuery.setInt(1, quantityToReduce);
                updateStockQuery.setInt(2, stockId);
                updateStockQuery.executeUpdate();
                updateStockQuery.close();

                PreparedStatement updateStockShelfQuery = connection.prepareStatement(
                        "INSERT INTO shelf_stock (stock_id, shelf_id, quantity_moved) VALUES (?, ?, ?)"
                );
                updateStockShelfQuery.setInt(1, stockId);
                updateStockShelfQuery.setInt(2, shelf_id);
                updateStockShelfQuery.setInt(3, quantityToReduce);
                updateStockShelfQuery.executeUpdate();
                updateStockShelfQuery.close();

                remainingQuantity -= quantityToReduce;
            }

            stockOfItem.close();
            getStockQuery.close();

            if (remainingQuantity == 0) {
                connection.commit();
            } else {
                connection.rollback();
            }
        }
        catch (SQLException e)
        {
            connection.rollback();
            throw e;
        }
        finally {
            connection.setAutoCommit(true);
            connection.close();
        }
    }

    // Smart stock allocation that picks the best batches to use first
    public void allocateStockToShelf(Item item, int quantity, String shelfType) 
            throws SQLException, ClassNotFoundException {
        
        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();
        
        try {
            connection.setAutoCommit(false);
            
            PreparedStatement getStockQuery = connection.prepareStatement(
                    "SELECT id, quantity, date_of_expiry, date_of_purchase FROM stock " +
                    "WHERE item_id = ? AND quantity > 0 " +
                    "ORDER BY date_of_purchase ASC" // Order by purchase date (FIFO)
            );
            getStockQuery.setInt(1, item.getId());
            ResultSet stockBatches = getStockQuery.executeQuery();
            
            int remainingQuantity = quantity;
            
            while (stockBatches.next() && remainingQuantity > 0) {
                int stockId = stockBatches.getInt("id");
                int stockQuantity = stockBatches.getInt("quantity");
                java.sql.Date expiryDate = stockBatches.getDate("date_of_expiry");
                java.sql.Date purchaseDate = stockBatches.getDate("date_of_purchase");
                
                // Check if there's a newer batch with closer expiry date
                PreparedStatement checkNewerBatchQuery = connection.prepareStatement(
                        "SELECT id, quantity FROM stock " +
                        "WHERE item_id = ? AND quantity > 0 AND date_of_purchase > ? " +
                        "AND date_of_expiry < ? " +
                        "ORDER BY date_of_expiry ASC LIMIT 1"
                );
                checkNewerBatchQuery.setInt(1, item.getId());
                checkNewerBatchQuery.setDate(2, purchaseDate);
                checkNewerBatchQuery.setDate(3, expiryDate);
                
                ResultSet newerBatch = checkNewerBatchQuery.executeQuery();
                
                int selectedStockId = stockId;
                int selectedStockQuantity = stockQuantity;
                
                if (newerBatch.next()) {
                    selectedStockId = newerBatch.getInt("id");
                    selectedStockQuantity = newerBatch.getInt("quantity");
                }
                
                int quantityToReduce = Math.min(remainingQuantity, selectedStockQuantity);
                
                // Update stock quantity
                PreparedStatement updateStockQuery = connection.prepareStatement(
                        "UPDATE stock SET quantity = quantity - ? WHERE id = ?"
                );
                updateStockQuery.setInt(1, quantityToReduce);
                updateStockQuery.setInt(2, selectedStockId);
                updateStockQuery.executeUpdate();
                
                // Update shelf inventory
                PreparedStatement updateShelfQuery = connection.prepareStatement(
                        "INSERT INTO shelf (item_id, quantity, type) VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE quantity = quantity + ?"
                );
                updateShelfQuery.setInt(1, item.getId());
                updateShelfQuery.setInt(2, quantityToReduce);
                updateShelfQuery.setString(3, shelfType);
                updateShelfQuery.setInt(4, quantityToReduce);
                updateShelfQuery.executeUpdate();
                
                remainingQuantity -= quantityToReduce;
                
                newerBatch.close();
                checkNewerBatchQuery.close();
                updateStockQuery.close();
                updateShelfQuery.close();
            }
            
            if (remainingQuantity == 0) {
                connection.commit();
            } else {
                connection.rollback();
            }
            
            stockBatches.close();
            getStockQuery.close();
            
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
            connection.close();
        }
    }
}
