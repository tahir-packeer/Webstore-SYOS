package org.example.presentation.controllers;

import org.example.persistence.database.DatabaseConnection;
import org.example.persistence.models.User;
import org.example.presentation.views.Admin;
import org.example.presentation.views.Cashier;
import org.example.presentation.views.StoreManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Scanner;

public class Authentication {

    public static void startLoginProcess() throws SQLException, ClassNotFoundException, ParseException {
        try (Scanner input = new Scanner(System.in)) {
            clearScreen();
            displaySecurityBanner();

            System.out.println("...................................................");
            System.out.println("          Please verify your identity           ");
            System.out.println("...................................................");
            System.out.println();

            System.out.print("Staff Username: ");
            String username = input.nextLine();

            System.out.print("Access password: ");
            String password = input.nextLine();

            Authentication auth = new Authentication();
            User user = auth.authenticateUser(username, password);

            // Route user to interface based on their role
            if (user != null) {
                System.out.println("Authentication Successful!");
                System.out.println("Access Granted for: " + user.getName());
                Thread.sleep(1000);

                String userType = user.getType();
                switch (userType.toLowerCase()) {
                    case "cashier":
                        System.out.println("Loading Cashier Terminal...");
                        new Cashier().cashierInterface(user);
                        break;
                    case "store manager":
                        System.out.println("Loading Manager Dashboard...");
                        StoreManager storeManager = new StoreManager();
                        storeManager.storeManagerDashboard(user);
                        break;
                    case "manager":
                    case "admin":
                        System.out.println("Loading Administrative Panel...");
                        Admin admin = new Admin();
                        admin.adminInterface(user);
                        break;
                    default:
                        System.out.println("Unrecognized access level: " + userType);
                }
            } else {
                System.out.println("Authentication Failed!");
                System.out.println("......................................");
                System.out.println("     Would you like to retry?       ");
                System.out.println("       [Y] Yes, try again           ");
                System.out.println("       [N] No, exit system          ");
                System.out.println("......................................");
                System.out.print(" Your choice : ");
                String retry = input.nextLine();
                if (retry.equalsIgnoreCase("yes") || retry.equalsIgnoreCase("y")) {
                    startLoginProcess();
                } else {
                    System.out.println("System Logout Complete. Have a great day!");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[2J\033[H");
            }
        } catch (Exception e) {
            // If clear doesn't work, just add some space
            for (int i = 0; i < 30; i++) {
                System.out.println();
            }
        }
    }

    private static void displaySecurityBanner() {
        System.out.println("...........................................................");
        System.out.println("                   EMPLOYEE PORTAL                     ");
        System.out.println("...........................................................");
        System.out.println();
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
                    System.out.println("Access Denied - Invalid credentials detected");
                }
            } finally {
                if (resultSet != null)
                    resultSet.close();
                if (statement != null)
                    statement.close();
                db.closeConnection(connection);
            }

        } catch (ClassNotFoundException e) {
            System.out.println("System Error: Database driver unavailable");
        } catch (SQLException e) {
            System.out.println("System Error: Database connection interrupted");
        }

        return null;
    }
}
