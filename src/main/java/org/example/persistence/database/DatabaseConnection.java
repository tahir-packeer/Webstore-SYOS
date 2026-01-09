package org.example.persistence.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private static final Object lock = new Object();

    // Database configuration with connection pooling
    private final String url = "jdbc:mysql://localhost:3306/syos?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&maxActive=100&maxIdle=30&minIdle=5&initialSize=10&removeAbandonedTimeout=60&removeAbandoned=true";
    private final String username = "root";
    private final String password = "9900@tahir";

    private DatabaseConnection() {
        // Private constructor to prevent instantiation
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    public Connection connect() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");

        // Connection properties for better performance under load
        Properties props = new Properties();
        props.setProperty("user", username);
        props.setProperty("password", password);
        props.setProperty("useSSL", "false");
        props.setProperty("allowPublicKeyRetrieval", "true");
        props.setProperty("serverTimezone", "UTC");
        props.setProperty("autoReconnect", "true");
        props.setProperty("maxReconnects", "3");
        props.setProperty("initialTimeout", "2");

        return DriverManager.getConnection(url, props);
    }

    // Clean up database connections to prevent memory leaks
    public void closeConnection(Connection connection) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Failed to close connection: " + e.getMessage());
        }
    }

}
