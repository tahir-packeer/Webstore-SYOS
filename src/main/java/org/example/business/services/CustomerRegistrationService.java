package org.example.business.services;

import org.example.persistence.models.Customer;
import org.example.persistence.database.DatabaseConnection;

import java.sql.*;

public class CustomerRegistrationService {

    public int registerCustomer(String name, String contactNumber) {
        // Check if customer already exists
        if (isPhoneNumberExists(contactNumber)) {
            System.out.println("Customer with phone number " + contactNumber + " already exists");
            return -1;
        }

        String sql = "INSERT INTO customers (name, contactNumber) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getInstance().connect();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, name);
            pstmt.setString(2, contactNumber);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int customerId = rs.getInt(1);
                        System.out.println("Customer registered successfully. ID: " + customerId);
                        return customerId;
                    }
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error registering customer: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Login customer by phone number
     */
    public Customer loginCustomer(String contactNumber) {
        String sql = "SELECT * FROM customers WHERE contactNumber = ?";

        try (Connection conn = DatabaseConnection.getInstance().connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, contactNumber);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCustomer(rs);
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error finding customer: " + e.getMessage());
        }
        return null;
    }

    /**
     * Check if phone number already exists
     */
    private boolean isPhoneNumberExists(String contactNumber) {
        String sql = "SELECT COUNT(*) FROM customers WHERE contactNumber = ?";

        try (Connection conn = DatabaseConnection.getInstance().connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, contactNumber);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error checking phone number existence: " + e.getMessage());
        }
        return false;
    }

    // Helper method to map ResultSet to Customer object
    private Customer mapResultSetToCustomer(ResultSet rs) throws SQLException {
        Customer customer = new Customer(
                rs.getString("name"),
                rs.getString("contactNumber"));
        customer.setId(rs.getInt("id"));

        String email = rs.getString("email");
        String address = rs.getString("address");
        if (email != null)
            customer.setEmail(email);
        if (address != null)
            customer.setAddress(address);

        return customer;
    }
}
