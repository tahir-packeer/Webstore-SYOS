package org.example.presentation.controllers;

import org.example.persistence.database.DatabaseConnection;
import org.example.persistence.models.*;
import org.example.business.validators.CustomerValidator;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OnlineController {
    // Retrieve a single online customer by contact number
    public Customer getOnlineCustomerByContactNumber(String contactNumber) throws SQLException, ClassNotFoundException {
        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();
        try {
            String query = "SELECT * FROM online_customers WHERE contactNumber = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, contactNumber);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        Customer customer = new Customer(
                                rs.getString("name"),
                                rs.getString("contactNumber"),
                                rs.getString("email"),
                                rs.getString("address"));
                        customer.setId(rs.getInt("id"));
                        return customer;
                    }
                }
            }
        } finally {
            connection.close();
        }
        return null;
    }

    // Retrieve all online customers
    public List<Customer> getAllOnlineCustomers() throws SQLException, ClassNotFoundException {
        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();
        List<Customer> customers = new ArrayList<>();
        try {
            String query = "SELECT * FROM online_customers";
            try (PreparedStatement statement = connection.prepareStatement(query);
                    ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Customer customer = new Customer(
                            rs.getString("name"),
                            rs.getString("contactNumber"),
                            rs.getString("email"),
                            rs.getString("address"));
                    customer.setId(rs.getInt("id"));
                    customers.add(customer);
                }
            }
        } finally {
            connection.close();
        }
        return customers;
    }

    // Register a new online customer
    public boolean registerOnlineCustomer(Customer customer, String email, String address)
            throws SQLException, ClassNotFoundException {
        return registerOnlineCustomer(customer, email, address, "defaultpass123");
    }

    // Register a new online customer with password
    public boolean registerOnlineCustomer(Customer customer, String email, String address, String password)
            throws SQLException, ClassNotFoundException {
        // Validate customer data before processing
        CustomerValidator.ValidationResult validation = CustomerValidator.validateOnlineCustomerData(
                customer.getName(),
                customer.getcontactNumber(),
                email,
                address);

        if (!validation.isValid()) {
            System.out.println("Customer validation failed: " + validation.getErrors());
            throw new IllegalArgumentException("Invalid customer data: " + validation.getErrors());
        }

        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();

        String query = "INSERT INTO online_customers (name, contactNumber, email, address, password, registrationDate) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, customer.getName().trim());
            statement.setString(2, customer.getcontactNumber().trim());
            statement.setString(3, email.trim());
            statement.setString(4, address.trim());
            statement.setString(5, password);
            statement.setTimestamp(6, Timestamp.valueOf(java.time.LocalDateTime.now()));

            int rowsInserted = statement.executeUpdate();

            if (rowsInserted > 0) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        customer.setId(generatedKeys.getInt(1));
                        System.out.println("Online customer registered successfully with ID: " + customer.getId());
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error registering online customer: " + e.getMessage());
            throw e;
        } finally {
            connection.close();
        }

        return false;
    }

    // Process online order/bill
    public Bill processOnlineOrder(List<BillItem> orderItems, Customer customer, double discount)
            throws SQLException, ClassNotFoundException {

        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();

        try {
            connection.setAutoCommit(false);

            // Calculate total
            double fullPrice = orderItems.stream()
                    .mapToDouble(BillItem::getTotalPrice)
                    .sum();

            // Generate invoice number
            String invoiceNumber = new BillController().getInvoiceNumber();

            // Create online bill (no cash transaction for online orders)
            Bill bill = new Bill(customer, invoiceNumber, fullPrice - discount, discount,
                    0, 0, "ONLINE", "WEBSITE");

            // Insert bill
            String billQuery = "INSERT INTO bill (customer_id, customer_type, invoiceNumber, fullPrice, discount, cashTendered, changeAmount, billDate, transactionType, storeType) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement billStatement = connection.prepareStatement(billQuery,
                    Statement.RETURN_GENERATED_KEYS)) {
                billStatement.setInt(1, customer.getId());
                billStatement.setString(2, "ONLINE");
                billStatement.setString(3, bill.getInvoiceNumber());
                billStatement.setDouble(4, bill.getFullPrice());
                billStatement.setDouble(5, bill.getDiscount());
                billStatement.setDouble(6, bill.getCashTendered());
                billStatement.setDouble(7, bill.getChangeAmount());
                billStatement.setObject(8, bill.getBillDate());
                billStatement.setString(9, bill.getTransactionType());
                billStatement.setString(10, bill.getStoreType());

                int rowsInserted = billStatement.executeUpdate();
                if (rowsInserted > 0) {
                    try (ResultSet generatedKeys = billStatement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            bill.setId(generatedKeys.getInt(1));
                        }
                    }
                }
            }

            // Insert bill items and update website inventory
            String billItemQuery = "INSERT INTO billItem (item_id, bill_id, quantity, itemPrice, totalPrice) VALUES (?, ?, ?, ?, ?)";
            String updateWebsiteInventoryQuery = "UPDATE shelf SET quantity = quantity - ? WHERE item_id = ? AND type = 'WEBSITE' AND quantity >= ?";

            try (PreparedStatement billItemStatement = connection.prepareStatement(billItemQuery);
                    PreparedStatement inventoryStatement = connection.prepareStatement(updateWebsiteInventoryQuery)) {

                for (BillItem item : orderItems) {
                    // Insert bill item
                    billItemStatement.setInt(1, item.getItem().getId());
                    billItemStatement.setInt(2, bill.getId());
                    billItemStatement.setInt(3, item.getQuantity());
                    billItemStatement.setDouble(4, item.getItemPrice());
                    billItemStatement.setDouble(5, item.getTotalPrice());
                    billItemStatement.executeUpdate();

                    // Update website inventory
                    inventoryStatement.setInt(1, item.getQuantity());
                    inventoryStatement.setInt(2, item.getItem().getId());
                    inventoryStatement.setInt(3, item.getQuantity());
                    int inventoryUpdated = inventoryStatement.executeUpdate();

                    if (inventoryUpdated == 0) {
                        throw new SQLException("Insufficient website inventory for item: " + item.getItem().getName());
                    }
                }
            }

            connection.commit();
            System.out.println("Online order processed successfully. Order ID: " + bill.getId());
            return bill;

        } catch (SQLException e) {
            connection.rollback();
            System.out.println("Error processing online order: " + e.getMessage());
            throw e;
        } finally {
            connection.setAutoCommit(true);
            connection.close();
        }
    }

    // Get available items for website
    public List<Item> getWebsiteInventory() throws SQLException, ClassNotFoundException {
        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();
        List<Item> availableItems = new ArrayList<>();

        String query = """
                    SELECT i.id, i.code, i.name, i.price, s.quantity
                    FROM items i
                    JOIN shelf s ON i.id = s.item_id
                    WHERE s.type = 'WEBSITE' AND s.quantity > 0
                    ORDER BY i.name ASC
                """;

        try (PreparedStatement statement = connection.prepareStatement(query);
                ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Item item = new Item(
                        resultSet.getString("code"),
                        resultSet.getString("name"),
                        resultSet.getDouble("price"));
                item.setId(resultSet.getInt("id"));
                availableItems.add(item);
            }
        } finally {
            connection.close();
        }

        return availableItems;
    }

    // Check if enough stock is available for online order
    public boolean isItemAvailableOnline(int itemId, int quantity) throws SQLException, ClassNotFoundException {
        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();

        String query = "SELECT quantity FROM shelf WHERE item_id = ? AND type = 'WEBSITE'";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, itemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int availableQuantity = resultSet.getInt("quantity");
                    return availableQuantity >= quantity;
                }
            }
        } finally {
            connection.close();
        }

        return false;
    }
}
