package org.example.Controller;

import org.example.Database.DatabaseConnection;
import org.example.Model.User;
import org.example.View.Admin;
import org.example.View.Cashier;
import org.example.View.StoreManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Scanner;

public class Authentication {

    public static void startLoginProcess() throws SQLException, ClassNotFoundException, ParseException {
        try (Scanner input = new Scanner(System.in)) {
            System.out.println("\nWelcome to Synex Outlet Store");
            System.out.println("\nPlease login to continue");

            System.out.print("Enter your Name: ");
            String username = input.nextLine();

            System.out.print("Enter your Password: ");
            String password = input.nextLine();

            Authentication auth = new Authentication();
            User user = auth.authenticateUser(username, password);

            // Route user to appropriate interface based on their role
            if (user != null) {
                String userType = user.getType();
                switch (userType.toLowerCase()) {
                    case "cashier":
                        System.out.println("Welcome Cashier: " + user.getName());
                        new Cashier().cashierInterface(user);
                        break;
                    case "storemanager":
                        StoreManager storeManager = new StoreManager();
                        storeManager.storeManagerDashboard(user);
                        break;
                    case "manager":
                        Admin admin = new Admin();
                        admin.adminInterface(user);
                        break;
                    default:
                        System.out.println("Unknown user type: " + userType);
                }
            } else {
                System.out.print("Login failed. Would you like to try again? (yes/no): ");
                String retry = input.nextLine();
                if (retry.equalsIgnoreCase("yes") || retry.equalsIgnoreCase("y")) {
                    startLoginProcess(); // Retry login if user wants to
                } else {
                    System.out.println("Exiting application. Goodbye!");
                }
            }
        }
    }

    public User authenticateUser(String username, String password) {
        DatabaseConnection db = DatabaseConnection.getInstance();
        try {
            Connection connection = db.connect();
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            User user = null;

            try {
                String query = "SELECT * FROM users WHERE name=? AND password=?";
                statement = connection.prepareStatement(query);
                statement.setString(1, username);
                statement.setString(2, password);
                resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    String id = resultSet.getString("id");
                    String name = resultSet.getString("name");
                    String type = resultSet.getString("type");

                    user = new User(id, name, password, type);
                    return user;
                } else {
                    System.out.println("Invalid username or password");
                }
            } finally {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
                db.closeConnection(connection);
            }

        } catch (ClassNotFoundException e) {
            System.out.println("Driver not found");
        } catch (SQLException e) {
            System.out.println("Connection failed");
        }

        return null;
    }
}
