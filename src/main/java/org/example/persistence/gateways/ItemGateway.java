package org.example.persistence.gateways;

import org.example.persistence.database.DatabaseConnection;
import org.example.shared.dto.ItemDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemGateway {
    private static ItemGateway instance;
    private static final Object lock = new Object();
    private final DatabaseConnection dbConnection;

    private ItemGateway() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public static ItemGateway getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ItemGateway();
                }
            }
        }
        return instance;
    }

    public void insert(ItemDTO item) throws SQLException, ClassNotFoundException {
        Connection connection = dbConnection.connect();
        String query = "INSERT INTO items (code, name, price) VALUES (?, ?, ?)";
        
        try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, item.getCode());
            statement.setString(2, item.getName());
            statement.setDouble(3, item.getPrice());
            statement.executeUpdate();
            
            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                item.setId(generatedKeys.getInt(1));
            }
        } finally {
            dbConnection.closeConnection(connection);
        }
    }

    public void insertBatch(List<ItemDTO> items) throws SQLException, ClassNotFoundException {
        Connection connection = dbConnection.connect();
        String query = "INSERT INTO items (code, name, price) VALUES (?, ?, ?)";
        
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            for (ItemDTO item : items) {
                statement.setString(1, item.getCode());
                statement.setString(2, item.getName());
                statement.setDouble(3, item.getPrice());
                statement.addBatch();
            }
            statement.executeBatch();
        } finally {
            dbConnection.closeConnection(connection);
        }
    }

    public ItemDTO findByCode(String code) throws SQLException, ClassNotFoundException {
        Connection connection = dbConnection.connect();
        String query = "SELECT * FROM items WHERE code = ?";
        ItemDTO item = null;
        
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, code);
            ResultSet resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                item = mapResultSetToDTO(resultSet);
            }
        } finally {
            dbConnection.closeConnection(connection);
        }
        
        return item;
    }

    public ItemDTO findById(int id) throws SQLException, ClassNotFoundException {
        Connection connection = dbConnection.connect();
        String query = "SELECT * FROM items WHERE id = ?";
        ItemDTO item = null;
        
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                item = mapResultSetToDTO(resultSet);
            }
        } finally {
            dbConnection.closeConnection(connection);
        }
        
        return item;
    }

    public List<ItemDTO> findAll() throws SQLException, ClassNotFoundException {
        Connection connection = dbConnection.connect();
        String query = "SELECT * FROM items ORDER BY name";
        List<ItemDTO> items = new ArrayList<>();
        
        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            
            while (resultSet.next()) {
                items.add(mapResultSetToDTO(resultSet));
            }
        } finally {
            dbConnection.closeConnection(connection);
        }
        
        return items;
    }

    public void update(ItemDTO item) throws SQLException, ClassNotFoundException {
        Connection connection = dbConnection.connect();
        String query = "UPDATE items SET code = ?, name = ?, price = ? WHERE id = ?";
        
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, item.getCode());
            statement.setString(2, item.getName());
            statement.setDouble(3, item.getPrice());
            statement.setInt(4, item.getId());
            statement.executeUpdate();
        } finally {
            dbConnection.closeConnection(connection);
        }
    }

    public boolean exists(String code) throws SQLException, ClassNotFoundException {
        Connection connection = dbConnection.connect();
        String query = "SELECT COUNT(*) FROM items WHERE code = ?";
        
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, code);
            ResultSet resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        } finally {
            dbConnection.closeConnection(connection);
        }
        
        return false;
    }

    private ItemDTO mapResultSetToDTO(ResultSet resultSet) throws SQLException {
        return new ItemDTO(
            resultSet.getInt("id"),
            resultSet.getString("code"),
            resultSet.getString("name"),
            resultSet.getDouble("price")
        );
    }
}
