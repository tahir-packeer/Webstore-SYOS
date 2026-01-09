
package org.example.presentation.servlets;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.BufferedReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.ArrayList;

import org.example.presentation.controllers.ItemController;
import org.example.presentation.controllers.BillController;
import org.example.presentation.controllers.CustomerController;
import org.example.persistence.models.BillItem;
import org.example.persistence.models.Customer;
import org.example.persistence.models.Item;
import org.example.persistence.models.Bill;


public class SalesServlet extends HttpServlet {
    // Handles sales and billing operations
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
            JSONObject obj = new JSONObject(json);
            JSONArray itemsArr = obj.getJSONArray("items");
            double cashTendered = obj.optDouble("cashTendered", 0);
            double discount = obj.optDouble("discount", 0);
            
            // Handle customerId as string (phone number) or integer
            String customerIdentifier = null;
            if (obj.has("customerId") && !obj.isNull("customerId")) {
                try {
                    customerIdentifier = obj.getString("customerId");
                } catch (Exception e) {
                    // If getString fails, try getting as object and convert to string
                    Object customerIdObj = obj.get("customerId");
                    customerIdentifier = customerIdObj.toString();
                }
            }

            // Use controllers and models
            ItemController itemController = new ItemController();
            BillController billController = new BillController();

            List<BillItem> billItems = new ArrayList<>();
            double total = 0;
            Customer customer = null;
            if (customerIdentifier != null) {
                CustomerController customerController = new CustomerController();
                System.out.println("Looking up in-store customer with identifier: " + customerIdentifier);
                
                // Primary lookup: regular customers table (for in-store customers)
                try {
                    customer = customerController.get_Customer_from_contactNumber(customerIdentifier);
                    if (customer != null) {
                        System.out.println("In-store customer found: " + customer.getName() + " (ID: " + customer.getId() + ")");
                    }
                } catch (Exception e) {
                    System.out.println("Customer lookup in customers table failed: " + e.getMessage());
                }
                
                // Fallback: check online customers table (in case they're also registered online)
                if (customer == null) {
                    try {
                        customer = customerController.get_OnlineCustomer_from_contactNumber(customerIdentifier);
                        if (customer != null) {
                            System.out.println("Online customer found for in-store transaction: " + customer.getName() + " (ID: " + customer.getId() + ")");
                        }
                    } catch (Exception e) {
                        System.out.println("Customer lookup in online_customers table failed: " + e.getMessage());
                    }
                }
            }
            
            // First pass: Validate all items and quantities before processing
            for (int i = 0; i < itemsArr.length(); i++) {
                JSONObject itemObj = itemsArr.getJSONObject(i);
                String code = itemObj.getString("code");
                int qty = itemObj.getInt("quantity");
                Item item = itemController.getItemFromCode(code);
                if (item == null) {
                    resp.setStatus(400);
                    resp.getWriter().write("{\"error\":\"Item code not found: " + code + "\"}");
                    return;
                }
                
                // Check if enough quantity is available on store shelf (for POS transactions)
                int availableQuantity = getStoreShelfQuantity(item.getId());
                if (availableQuantity < qty) {
                    resp.setStatus(400);
                    resp.getWriter().write("{\"error\":\"Insufficient stock for " + item.getName() + ". Available: " + availableQuantity + ", Requested: " + qty + "\"}");
                    return;
                }
            }
            
            // Second pass: Create bill items (only if all validations passed)
            for (int i = 0; i < itemsArr.length(); i++) {
                JSONObject itemObj = itemsArr.getJSONObject(i);
                String code = itemObj.getString("code");
                int qty = itemObj.getInt("quantity");
                Item item = itemController.getItemFromCode(code);
                if (item == null) {
                    resp.setStatus(400);
                    resp.getWriter().write("{\"error\":\"Item code not found: " + code + "\"}");
                    return;
                }
                BillItem billItem = new BillItem(item, qty);
                billItems.add(billItem);
                total += billItem.getTotalPrice();
            }
            String invoiceNumber = billController.getInvoiceNumber();
            double finalTotal = total - discount;
            double change = cashTendered - finalTotal;
            Bill bill = new Bill(customer, invoiceNumber, total, discount, cashTendered, change);
            bill = billController.Add_Bill(bill);
            billController.add_Bill_items(billItems, bill);

            JSONObject result = new JSONObject();
            result.put("billId", bill.getId());
            result.put("invoiceNumber", bill.getInvoiceNumber());
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
        String billIdParam = req.getParameter("id");
    try (Connection conn = org.example.persistence.database.DatabaseConnection.getInstance().connect()) {
            if (billIdParam != null) {
                // Fetch single bill details
                int billId = Integer.parseInt(billIdParam);
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM bill WHERE id = ?");
                ps.setInt(1, billId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    resp.setStatus(404);
                    resp.getWriter().write("{\"error\":\"Bill not found\"}");
                    return;
                }
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
                // Fetch bill items
                PreparedStatement itemsPs = conn.prepareStatement("SELECT bi.*, i.code, i.name FROM billItem bi JOIN items i ON bi.item_id = i.id WHERE bi.bill_id = ?");
                itemsPs.setInt(1, billId);
                ResultSet itemsRs = itemsPs.executeQuery();
                JSONArray itemsArr = new JSONArray();
                while (itemsRs.next()) {
                    JSONObject itemObj = new JSONObject();
                    itemObj.put("code", itemsRs.getString("code"));
                    itemObj.put("name", itemsRs.getString("name"));
                    itemObj.put("quantity", itemsRs.getInt("quantity"));
                    itemObj.put("itemPrice", itemsRs.getDouble("itemPrice"));
                    itemObj.put("totalPrice", itemsRs.getDouble("totalPrice"));
                    itemsArr.put(itemObj);
                }
                billObj.put("items", itemsArr);
                itemsRs.close();
                itemsPs.close();
                resp.getWriter().write(billObj.toString());
            } else {
                // List bills (optionally filter by date, cashier, etc.)
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM bill ORDER BY billDate DESC LIMIT 100");
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
            }
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        String billIdParam = req.getParameter("id");
        if (billIdParam == null) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"Missing bill id parameter\"}");
            return;
        }
        try (Connection conn = org.example.persistence.database.DatabaseConnection.getInstance().connect()) {
            int billId = Integer.parseInt(billIdParam);
            PreparedStatement delItems = conn.prepareStatement("DELETE FROM billItem WHERE bill_id = ?");
            delItems.setInt(1, billId);
            delItems.executeUpdate();
            delItems.close();
            PreparedStatement delBill = conn.prepareStatement("DELETE FROM bill WHERE id = ?");
            delBill.setInt(1, billId);
            int affected = delBill.executeUpdate();
            delBill.close();
            if (affected == 0) {
                resp.setStatus(404);
                resp.getWriter().write("{\"error\":\"Bill not found\"}");
            } else {
                resp.getWriter().write("{\"success\":true}");
            }
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
    
    // Helper method to check available quantity on store shelf (for POS transactions)
    private int getStoreShelfQuantity(int itemId) throws Exception {
        try (Connection conn = org.example.persistence.database.DatabaseConnection.getInstance().connect()) {
            String query = "SELECT quantity FROM shelf WHERE item_id = ? AND type = 'STORE'";
            try (PreparedStatement statement = conn.prepareStatement(query)) {
                statement.setInt(1, itemId);
                
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("quantity");
                    }
                }
            }
        }
        return 0; // Item not found on store shelf
    }
}
