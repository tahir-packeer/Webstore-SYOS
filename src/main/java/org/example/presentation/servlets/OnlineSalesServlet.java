
package org.example.presentation.servlets;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.BufferedReader;
import java.util.List;
import java.util.ArrayList;

import org.example.presentation.controllers.CustomerController;
import org.example.presentation.controllers.ItemController;
import org.example.presentation.controllers.BillController;
import org.example.persistence.models.Customer;
import org.example.persistence.models.Item;
import org.example.persistence.models.BillItem;
import org.example.persistence.models.Bill;
import org.example.persistence.database.DatabaseConnection;

public class OnlineSalesServlet extends HttpServlet {
    // Handles online sales operations
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            BufferedReader reader = req.getReader();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String json = sb.toString();

            JSONObject obj;
            try {
                obj = new JSONObject(json);
            } catch (Exception e) {
                System.out.println("Failed to parse JSON: " + e.getMessage());
                resp.setStatus(400);
                resp.getWriter().write("{\"error\":\"Invalid JSON format: " + e.getMessage() + "\"}");
                return;
            }

            // Get customer identifier - handle both string and int types
            String customerIdentifier = "";
            if (obj.has("customerId")) {
                // Use getString() to safely handle both string and numeric values
                try {
                    customerIdentifier = obj.getString("customerId");
                } catch (Exception e) {
                    System.out.println("Failed to get customerId as string, trying as object: " + e.getMessage());
                    Object customerIdObj = obj.get("customerId");
                    customerIdentifier = customerIdObj.toString();
                }
            }

            if (customerIdentifier.isEmpty()) {
                resp.setStatus(400);
                resp.getWriter().write("{\"error\":\"Customer ID is required\"}");
                return;
            }

            JSONArray itemsArr = obj.getJSONArray("items");
            double cashTendered = obj.optDouble("cashTendered", 0);
            double discount = obj.optDouble("discount", 0);

            CustomerController customerController = new CustomerController();

            Customer customer = null;

            // Primary lookup: online_customers table (for e-commerce customers)
            try {
                customer = customerController.get_OnlineCustomer_from_contactNumber(customerIdentifier);
                if (customer != null) {
                    System.out.println(
                            "Online customer found: " + customer.getName() + " (ID: " + customer.getId() + ")");
                }
            } catch (Exception e) {
                System.out.println("Customer lookup in online_customers table failed: " + e.getMessage());
            }

            // Fallback: check regular customer table (in case customer was registered
            // in-store first)
            if (customer == null) {
                try {
                    customer = customerController.get_Customer_from_contactNumber(customerIdentifier);
                    if (customer != null) {
                        System.out.println("Regular customer found (cross-lookup): " + customer.getName() + " (ID: "
                                + customer.getId() + ")");
                    }
                } catch (Exception e) {
                    System.out.println("Customer lookup in customer table failed: " + e.getMessage());
                }
            }

            // If still not found, try to find by ID if customerIdentifier is numeric
            if (customer == null) {
                try {
                    int customerId = Integer.parseInt(customerIdentifier);
                    customer = findCustomerById(customerId);
                    if (customer != null) {
                        System.out.println(
                                "Customer found by ID: " + customer.getName() + " (ID: " + customer.getId() + ")");
                    }
                } catch (NumberFormatException ex) {
                    System.out.println("customerIdentifier is not numeric: " + ex.getMessage());
                } catch (Exception ex) {
                    System.out.println("Customer lookup by ID also failed: " + ex.getMessage());
                }
            }

            if (customer == null) {
                resp.setStatus(400);
                resp.getWriter().write("{\"error\":\"Customer not found with identifier: " + customerIdentifier
                        + ". Please ensure you are logged in with a valid account.\"}");
                return;
            }

            ItemController itemController = new ItemController();
            List<BillItem> billItems = new ArrayList<>();
            double total = 0;

            // First pass: Validate all items and quantities before processing
            for (int i = 0; i < itemsArr.length(); i++) {
                JSONObject itemObj = itemsArr.getJSONObject(i);
                String code = itemObj.getString("code");
                int quantity = itemObj.getInt("quantity");
                Item item = itemController.getItemFromCode(code);
                if (item == null) {
                    resp.setStatus(400);
                    resp.getWriter().write("{\"error\":\"Item not found: " + code + "\"}");
                    return;
                }

                // Check if enough quantity is available on website shelf
                int availableQuantity = getWebsiteShelfQuantity(item.getId());
                if (availableQuantity < quantity) {
                    resp.setStatus(400);
                    resp.getWriter().write("{\"error\":\"Insufficient stock for " + item.getName() + ". Available: "
                            + availableQuantity + ", Requested: " + quantity + "\"}");
                    return;
                }
            }

            // Second pass: Create bill items (only if all validations passed)
            for (int i = 0; i < itemsArr.length(); i++) {
                JSONObject itemObj = itemsArr.getJSONObject(i);
                String code = itemObj.getString("code");
                int quantity = itemObj.getInt("quantity");
                Item item = itemController.getItemFromCode(code);
                BillItem billItem = new BillItem(item, quantity);
                billItems.add(billItem);
                total += billItem.getTotalPrice();
            }

            BillController billController = new BillController();
            String invoiceNumber = billController.getInvoiceNumber();
            double finalTotal = total - discount;
            double change = cashTendered - finalTotal;
            Bill bill = new Bill(customer, invoiceNumber, total, discount, cashTendered, change, "ONLINE", "WEBSITE");
            bill = billController.Add_Bill(bill);
            billController.add_Bill_items(billItems, bill);

            JSONObject result = new JSONObject();
            result.put("billId", bill.getId());
            result.put("invoiceNumber", invoiceNumber);
            result.put("total", total);
            result.put("discount", discount);
            result.put("finalTotal", finalTotal);
            result.put("cashTendered", cashTendered);
            result.put("change", change);
            resp.getWriter().write(result.toString());
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        try (Connection conn = org.example.persistence.database.DatabaseConnection.getInstance().connect()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM bill WHERE transactionType = 'ONLINE' ORDER BY billDate DESC LIMIT 100");
            ResultSet rs = ps.executeQuery();
            JSONArray billsArr = new JSONArray();
            while (rs.next()) {
                JSONObject billObj = new JSONObject();
                billObj.put("id", rs.getInt("id"));
                billObj.put("invoiceNumber", rs.getString("invoiceNumber"));
                billObj.put("fullPrice", rs.getDouble("fullPrice"));
                billObj.put("discount", rs.getDouble("discount"));
                billObj.put("cashTendered", rs.getDouble("cashTendered"));
                billObj.put("changeAmount", rs.getDouble("changeAmount"));
                billObj.put("billDate", rs.getDate("billDate"));
                billObj.put("transactionType", rs.getString("transactionType"));
                billObj.put("storeType", rs.getString("storeType"));
                billsArr.put(billObj);
            }
            resp.getWriter().write(billsArr.toString());
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    // Helper method to find customer by ID
    private Customer findCustomerById(int customerId) throws SQLException, ClassNotFoundException {
        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();

        try {
            String query = "SELECT * FROM customers WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, customerId);

                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        Customer customer = new Customer(
                                rs.getString("name"),
                                rs.getString("contact_number"));
                        customer.setId(rs.getInt("id"));
                        customer.setEmail(rs.getString("email"));
                        customer.setAddress(rs.getString("address"));
                        return customer;
                    }
                }
            }
        } finally {
            connection.close();
        }
        return null;
    }

    // Helper method to check available quantity on website shelf
    private int getWebsiteShelfQuantity(int itemId) throws SQLException, ClassNotFoundException {
        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();

        try {
            String query = "SELECT quantity FROM shelf WHERE item_id = ? AND type = 'WEBSITE'";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, itemId);

                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("quantity");
                    }
                }
            }
        } finally {
            connection.close();
        }
        return 0; // Item not found on website shelf
    }
}
