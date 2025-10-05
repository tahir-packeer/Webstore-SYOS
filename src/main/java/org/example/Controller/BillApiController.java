package org.example.Controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.Database.DatabaseConnection;
import org.example.Model.Bill;
import org.example.Model.BillItem;
import org.example.Model.Customer;
import org.example.Model.Item;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        
        String pathInfo = request.getPathInfo();
        
        try {
            if ("/next-number".equals(pathInfo)) {
                handleGetNextBillNumber(response);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\":\"Endpoint not found\"}");
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Internal server error\"}");
        }
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
                    customerData.getString("contactNumber")
                );
            }
            
            // Create Bill object with proper constructor
            Bill bill = new Bill(
                customer,
                billData.getString("invoiceNumber"),
                billData.getDouble("fullPrice"),
                billData.optDouble("discount", 0),
                billData.getDouble("cashTendered"),
                billData.getDouble("changeAmount"),
                billData.getString("transactionType"),
                billData.getString("storeType")
            );
            
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
                    "SELECT id, name, price FROM items WHERE code = ?"
                );
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
                    itemResult.getDouble("price")
                );
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
            
            // Return success response
            JSONObject responseObj = new JSONObject();
            responseObj.put("success", true);
            responseObj.put("invoiceNumber", savedBill.getInvoiceNumber());
            responseObj.put("billId", savedBill.getId());
            responseObj.put("total", billData.getDouble("fullPrice"));
            responseObj.put("change", billData.getDouble("changeAmount"));
            
            response.getWriter().write(responseObj.toString());
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Failed to create bill: " + e.getMessage() + "\"}");
        }
    }
    
    private void handleGetNextBillNumber(HttpServletResponse response) throws IOException {
        try {
            Connection conn = DatabaseConnection.getInstance().connect();
            // Get the highest bill number from database
            String sql = "SELECT MAX(CAST(SUBSTRING(invoiceNumber, 6) AS UNSIGNED)) as max_number " +
                        "FROM bill WHERE invoiceNumber LIKE 'SYOS-%'";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            
            int nextNumber = 1;
            if (rs.next()) {
                int maxNumber = rs.getInt("max_number");
                nextNumber = maxNumber + 1;
            }
            
            stmt.close();
            conn.close();
            
            PrintWriter out = response.getWriter();
            out.write("{\"nextNumber\":" + nextNumber + "}");
            
        } catch (Exception e) {
            // If bills table doesn't exist or any error, generate based on timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
            int fallbackNumber = Integer.parseInt(timestamp.substring(2)); // Use last 4 digits
            
            PrintWriter out = response.getWriter();
            out.write("{\"nextNumber\":" + fallbackNumber + "}");
        }
    }
}