package org.example.presentation.servlets;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseUtil {
    private static final String URL = "jdbc:mysql://localhost:3306/syos_db"; // Update with your DB name
    private static final String USER = "root"; // Update with your DB user
    private static final String PASSWORD = ""; // Update with your DB password

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
