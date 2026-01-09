package org.example.presentation.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.persistence.database.DatabaseConnection;
import org.example.persistence.models.Bill;
import org.example.persistence.models.BillItem;
import org.example.persistence.models.Customer;
import org.example.persistence.models.Item;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * REST API Controller for Bill operations
 * Handles bill creation and bill number generation
 */
@WebServlet("/api/bills/*")
public class BillApiController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // No GET endpoints needed - invoice numbers are generated server-side during
        // bill creation
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.getWriter().write("{\"error\":\"Endpoint not found\"}");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();

        try {
            if ("/create".equals(pathInfo)) {
                handleCreateBill(request, response);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\":\"Endpoint not found\"}");
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Internal server error\"}");
        }
    }

    private void handleCreateBill(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        // Read request body
        StringBuilder buffer = new StringBuilder();
        String line;
        try (var reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        }

        String requestBody = buffer.toString();
        JSONObject billData = new JSONObject(requestBody);
        BillController billController = new BillController();

        try {
            Connection conn = DatabaseConnection.getInstance().connect();

            // Create Customer object (can be null for cash sales)
            Customer customer = null;
            if (billData.has("customer") && !billData.isNull("customer")) {
                JSONObject customerData = billData.getJSONObject("customer");
                customer = new Customer(
                        customerData.getString("name"),
                        customerData.getString("contactNumber"));

                // Set customer ID if provided (for existing customers)
                if (customerData.has("id")) {
                    customer.setId(customerData.getInt("id"));
                }
            }

            // Generate invoice number on server side for consistency
            String invoiceNumber = generateNextInvoiceNumber();
            System.out.println("Generated new invoice number: " + invoiceNumber);

            // Create Bill object with proper constructor
            Bill bill = new Bill(
                    customer,
                    invoiceNumber,
                    billData.getDouble("fullPrice"),
                    billData.optDouble("discount", 0),
                    billData.getDouble("cashTendered"),
                    billData.getDouble("changeAmount"),
                    billData.getString("transactionType"),
                    billData.getString("storeType"));

            // Set bill date from string to LocalDate
            String billDateStr = billData.getString("billDate");
            bill.setBillDate(LocalDate.parse(billDateStr));

            // Save bill first to get bill ID
            Bill savedBill = billController.Add_Bill(bill);

            // Process bill items
            JSONArray items = billData.getJSONArray("items");
            List<BillItem> billItems = new ArrayList<>();

            for (int i = 0; i < items.length(); i++) {
                JSONObject itemData = items.getJSONObject(i);
                String itemCode = itemData.getString("code");

                // Look up the item ID from database using the item code
                PreparedStatement itemLookupStmt = conn.prepareStatement(
                        "SELECT id, name, price FROM items WHERE code = ?");
                itemLookupStmt.setString(1, itemCode);
                ResultSet itemResult = itemLookupStmt.executeQuery();

                if (!itemResult.next()) {
                    throw new Exception("Item not found: " + itemCode);
                }

                int itemId = itemResult.getInt("id");

                // Create Item object with existing constructor and set ID
                Item item = new Item(
                        itemCode,
                        itemResult.getString("name"),
                        itemResult.getDouble("price"));
                item.setId(itemId);

                // Create BillItem object
                BillItem billItem = new BillItem(item, itemData.getInt("quantity"));

                billItems.add(billItem);
                itemLookupStmt.close();
            }

            // Add bill items
            billController.add_Bill_items(billItems, savedBill);

            // Close connection
            conn.close();

            // Return success response with complete bill information
            JSONObject responseObj = new JSONObject();
            responseObj.put("success", true);
            responseObj.put("invoiceNumber", savedBill.getInvoiceNumber());
            responseObj.put("billId", savedBill.getId());
            responseObj.put("total", billData.getDouble("fullPrice"));
            responseObj.put("finalTotal", billData.getDouble("fullPrice") - billData.optDouble("discount", 0));
            responseObj.put("discount", billData.optDouble("discount", 0));
            responseObj.put("change", billData.getDouble("changeAmount"));
            responseObj.put("cashTendered", billData.getDouble("cashTendered"));
            responseObj.put("billDate", billDateStr);
            responseObj.put("transactionType", billData.getString("transactionType"));

            System.out.println("Bill added successfully with ID: " + savedBill.getId() + ", Invoice: "
                    + savedBill.getInvoiceNumber());

            response.getWriter().write(responseObj.toString());

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Failed to create bill: " + e.getMessage() + "\"}");
        }
    }

    private String generateNextInvoiceNumber() throws Exception {
        int nextNumber = getNextInvoiceNumber();
        return String.format("INV-%05d", nextNumber); // Format as INV-00001, INV-00002, etc.
    }

    private int getNextInvoiceNumber() throws Exception {
        Connection conn = DatabaseConnection.getInstance().connect();

        // Get the highest invoice number from both INV-XXXX and SYOS-XXXX formats
        String sql = """
                    SELECT MAX(invoice_num) as max_number FROM (
                        SELECT CAST(SUBSTRING(invoiceNumber, 5) AS UNSIGNED) as invoice_num
                        FROM bill WHERE invoiceNumber LIKE 'INV-%'
                        UNION ALL
                        SELECT CAST(SUBSTRING(invoiceNumber, 6) AS UNSIGNED) as invoice_num
                        FROM bill WHERE invoiceNumber LIKE 'SYOS-%'
                    ) combined_invoices
                """;

        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery();

        int nextNumber = 1; // Start from 1 if no invoices exist
        if (rs.next()) {
            int maxNumber = rs.getInt("max_number");
            if (!rs.wasNull()) {
                nextNumber = maxNumber + 1;
            }
        }

        stmt.close();
        conn.close();

        return nextNumber;
    }
}
