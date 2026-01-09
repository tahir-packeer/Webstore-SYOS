package org.example.persistence.gateways;

import org.example.persistence.database.DatabaseConnection;
import org.example.shared.dto.StockDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StockGateway {
    private static StockGateway instance;
    private static final Object lock = new Object();
    private final DatabaseConnection dbConnection;

    private StockGateway() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public static StockGateway getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new StockGateway();
                }
            }
        }
        return instance;
    }

    public void insert(StockDTO stock) throws SQLException, ClassNotFoundException {
        Connection connection = dbConnection.connect();
        String query = "INSERT INTO stock (item_id, quantity, date_of_expiry, date_of_purchase, availability) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, stock.getItemId());
            statement.setInt(2, stock.getQuantity());
            statement.setDate(3, Date.valueOf(stock.getDateOfExpiry()));
            statement.setDate(4, Date.valueOf(stock.getDateOfPurchase()));
            statement.setBoolean(5, stock.isAvailability());
            
            statement.executeUpdate();
            
            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                stock.setId(generatedKeys.getInt(1));
            }
        } finally {
            dbConnection.closeConnection(connection);
        }
    }

    public void update(StockDTO stock) throws SQLException, ClassNotFoundException {
        Connection connection = dbConnection.connect();
        String query = "UPDATE stock SET item_id = ?, quantity = ?, date_of_expiry = ?, date_of_purchase = ?, availability = ? WHERE id = ?";
        
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, stock.getItemId());
            statement.setInt(2, stock.getQuantity());
            statement.setDate(3, Date.valueOf(stock.getDateOfExpiry()));
            statement.setDate(4, Date.valueOf(stock.getDateOfPurchase()));
            statement.setBoolean(5, stock.isAvailability());
            statement.setInt(6, stock.getId());
            statement.executeUpdate();
        } finally {
            dbConnection.closeConnection(connection);
        }
    }

    public void updateQuantity(int stockId, int newQuantity) throws SQLException, ClassNotFoundException {
        Connection connection = dbConnection.connect();
        String query = "UPDATE stock SET quantity = ? WHERE id = ?";
        
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, newQuantity);
            statement.setInt(2, stockId);
            statement.executeUpdate();
        } finally {
            dbConnection.closeConnection(connection);
        }
    }

    public List<StockDTO> findByItemId(int itemId) throws SQLException, ClassNotFoundException {
        Connection connection = dbConnection.connect();
        String query = """
            SELECT s.*, i.code, i.name 
            FROM stock s 
            JOIN items i ON s.item_id = i.id 
            WHERE s.item_id = ? AND s.availability = true 
            ORDER BY 
                CASE 
                    WHEN s.date_of_expiry <= DATE_ADD(CURDATE(), INTERVAL 30 DAY) THEN 0
                    ELSE 1 
                END,
                s.date_of_expiry ASC, 
                s.date_of_purchase ASC
        """;
        List<StockDTO> stockList = new ArrayList<>();
        
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, itemId);
            ResultSet resultSet = statement.executeQuery();
            
            while (resultSet.next()) {
                stockList.add(mapResultSetToDTO(resultSet));
            }
        } finally {
            dbConnection.closeConnection(connection);
        }
        
        return stockList;
    }

    public List<StockDTO> findAll() throws SQLException, ClassNotFoundException {
        Connection connection = dbConnection.connect();
        String query = """
            SELECT s.*, i.code, i.name 
            FROM stock s 
            JOIN items i ON s.item_id = i.id 
            ORDER BY s.date_of_expiry ASC
        """;
        List<StockDTO> stockList = new ArrayList<>();
        
        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            
            while (resultSet.next()) {
                stockList.add(mapResultSetToDTO(resultSet));
            }
        } finally {
            dbConnection.closeConnection(connection);
        }
        
        return stockList;
    }

    public List<StockDTO> findLowStock(int threshold) throws SQLException, ClassNotFoundException {
        Connection connection = dbConnection.connect();
        String query = """
            SELECT s.*, i.code, i.name 
            FROM stock s 
            JOIN items i ON s.item_id = i.id 
            WHERE s.quantity <= ? AND s.availability = true 
            ORDER BY s.quantity ASC
        """;
        List<StockDTO> lowStockItems = new ArrayList<>();
        
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, threshold);
            ResultSet resultSet = statement.executeQuery();
            
            while (resultSet.next()) {
                lowStockItems.add(mapResultSetToDTO(resultSet));
            }
        } finally {
            dbConnection.closeConnection(connection);
        }
        
        return lowStockItems;
    }

    public int getTotalQuantityByItemId(int itemId) throws SQLException, ClassNotFoundException {
        Connection connection = dbConnection.connect();
        String query = "SELECT SUM(quantity) FROM stock WHERE item_id = ? AND availability = true";
        
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, itemId);
            ResultSet resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        } finally {
            dbConnection.closeConnection(connection);
        }
        
        return 0;
    }

    public boolean hasEnoughStock(int itemId, int requiredQuantity) throws SQLException, ClassNotFoundException {
        return getTotalQuantityByItemId(itemId) >= requiredQuantity;
    }

    public StockDTO getById(int id) throws SQLException, ClassNotFoundException {
        Connection connection = dbConnection.connect();
        String query = """
            SELECT s.*, i.code, i.name 
            FROM stock s 
            JOIN items i ON s.item_id = i.id 
            WHERE s.id = ?
        """;
        StockDTO stock = null;
        
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                stock = mapResultSetToDTO(resultSet);
            }
        } finally {
            dbConnection.closeConnection(connection);
        }
        
        return stock;
    }

    public List<StockDTO> getAllByItemId(int itemId) throws SQLException, ClassNotFoundException {
        Connection connection = dbConnection.connect();
        String query = """
            SELECT s.*, i.code, i.name 
            FROM stock s 
            JOIN items i ON s.item_id = i.id 
            WHERE s.item_id = ?
            ORDER BY s.date_of_expiry ASC, s.date_of_purchase ASC
        """;
        List<StockDTO> stockList = new ArrayList<>();
        
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, itemId);
            ResultSet resultSet = statement.executeQuery();
            
            while (resultSet.next()) {
                stockList.add(mapResultSetToDTO(resultSet));
            }
        } finally {
            dbConnection.closeConnection(connection);
        }
        
        return stockList;
    }

    public List<StockDTO> getAllStock() throws SQLException, ClassNotFoundException {
        return findAll();
    }

    private StockDTO mapResultSetToDTO(ResultSet resultSet) throws SQLException {
        return new StockDTO(
            resultSet.getInt("id"),
            resultSet.getInt("item_id"),
            resultSet.getString("code"),
            resultSet.getString("name"),
            resultSet.getInt("quantity"),
            resultSet.getDate("date_of_purchase").toLocalDate(),
            resultSet.getDate("date_of_expiry").toLocalDate(),
            resultSet.getBoolean("availability")
        );
    }
}
