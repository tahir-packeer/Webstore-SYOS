package org.example.presentation.servlets;

import org.example.persistence.database.DatabaseConnection;
import org.json.JSONObject;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/api/discount-codes/*")
public class DiscountServlet extends HttpServlet {
    private final DatabaseConnection dbConnection = DatabaseConnection.getInstance();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // List all discount codes
                listDiscountCodes(response);
            } else if (pathInfo.equals("/validate")) {
                // Validate discount code
                validateDiscountCode(request, response);
            } else {
                sendErrorResponse(response, "Invalid endpoint", 404);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, "Internal server error: " + e.getMessage(), 500);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // Create new discount code
                createDiscountCode(request, response);
            } else {
                sendErrorResponse(response, "Invalid endpoint", 404);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, "Internal server error: " + e.getMessage(), 500);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        
        try {
            if (pathInfo != null && pathInfo.length() > 1) {
                // Delete discount code by ID
                String idStr = pathInfo.substring(1); // Remove leading slash
                int id = Integer.parseInt(idStr);
                deleteDiscountCode(id, response);
            } else {
                sendErrorResponse(response, "Discount code ID is required", 400);
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(response, "Invalid discount code ID", 400);
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, "Internal server error: " + e.getMessage(), 500);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        
        try {
            if (pathInfo != null && pathInfo.length() > 1) {
                // Update discount code by ID
                String idStr = pathInfo.substring(1); // Remove leading slash
                int id = Integer.parseInt(idStr);
                updateDiscountCode(id, request, response);
            } else {
                sendErrorResponse(response, "Discount code ID is required", 400);
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(response, "Invalid discount code ID", 400);
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, "Internal server error: " + e.getMessage(), 500);
        }
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Handle CORS preflight requests
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private void validateDiscountCode(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String code = request.getParameter("code");
        
        if (code == null || code.trim().isEmpty()) {
            sendErrorResponse(response, "Discount code is required", 400);
            return;
        }

        code = code.trim().toUpperCase();
        
        Connection connection = dbConnection.connect();
        String query = "SELECT id, code, discount_value FROM discount_codes WHERE UPPER(code) = ? AND id IS NOT NULL";
        
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, code);
            ResultSet rs = statement.executeQuery();
            
            if (rs.next()) {
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("message", "Valid discount code");
                result.put("discount", rs.getDouble("discount_value"));
                result.put("code", rs.getString("code"));
                
                sendSuccessResponse(response, result.toString());
            } else {
                JSONObject result = new JSONObject();
                result.put("success", false);
                result.put("message", "Invalid or expired discount code");
                result.put("discount", 0);
                
                sendSuccessResponse(response, result.toString());
            }
        }
    }

    private void listDiscountCodes(HttpServletResponse response) throws Exception {
        Connection connection = dbConnection.connect();
        String query = "SELECT id, code, discount_value, created_date FROM discount_codes ORDER BY created_date DESC";
        
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            ResultSet rs = statement.executeQuery();
            
            JSONObject result = new JSONObject();
            org.json.JSONArray codes = new org.json.JSONArray();
            
            while (rs.next()) {
                JSONObject discount = new JSONObject();
                discount.put("id", rs.getInt("id"));
                discount.put("code", rs.getString("code"));
                discount.put("discount_value", rs.getDouble("discount_value"));
                discount.put("created_date", rs.getString("created_date"));
                codes.put(discount);
            }
            
            result.put("success", true);
            result.put("data", codes);
            
            sendSuccessResponse(response, result.toString());
        }
    }

    private void createDiscountCode(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Read JSON from request body
        StringBuilder requestBody = new StringBuilder();
        String line;
        while ((line = request.getReader().readLine()) != null) {
            requestBody.append(line);
        }
        
        JSONObject json = new JSONObject(requestBody.toString());
        String code = json.getString("code").trim().toUpperCase();
        double discountValue = json.getDouble("discount_value");
        
        if (code.isEmpty() || discountValue <= 0) {
            sendErrorResponse(response, "Invalid discount code or value", 400);
            return;
        }

        Connection connection = dbConnection.connect();
        
        // First check if the code already exists
        String checkQuery = "SELECT id FROM discount_codes WHERE UPPER(code) = ?";
        try (PreparedStatement checkStatement = connection.prepareStatement(checkQuery)) {
            checkStatement.setString(1, code);
            ResultSet rs = checkStatement.executeQuery();
            
            if (rs.next()) {
                sendErrorResponse(response, "Discount code already exists", 400);
                return;
            }
        }
        
        // Insert new discount code
        String query = "INSERT INTO discount_codes (code, discount_value) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, code);
            statement.setDouble(2, discountValue);
            
            int rowsAffected = statement.executeUpdate();
            
            if (rowsAffected > 0) {
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("message", "Discount code created successfully");
                
                sendSuccessResponse(response, result.toString());
            } else {
                sendErrorResponse(response, "Failed to create discount code", 500);
            }
        }
    }

    private void updateDiscountCode(int id, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Read JSON from request body
        StringBuilder requestBody = new StringBuilder();
        String line;
        while ((line = request.getReader().readLine()) != null) {
            requestBody.append(line);
        }
        
        JSONObject json = new JSONObject(requestBody.toString());
        String code = json.getString("code").trim().toUpperCase();
        double discountValue = json.getDouble("discount_value");
        
        if (code.isEmpty() || discountValue <= 0) {
            sendErrorResponse(response, "Invalid discount code or value", 400);
            return;
        }

        Connection connection = dbConnection.connect();
        
        // First check if the discount code exists
        String checkQuery = "SELECT id FROM discount_codes WHERE id = ?";
        try (PreparedStatement checkStatement = connection.prepareStatement(checkQuery)) {
            checkStatement.setInt(1, id);
            ResultSet rs = checkStatement.executeQuery();
            
            if (!rs.next()) {
                sendErrorResponse(response, "Discount code not found", 404);
                return;
            }
        }
        
        // Check if another discount code with the same code already exists (excluding current one)
        String duplicateCheckQuery = "SELECT id FROM discount_codes WHERE UPPER(code) = ? AND id != ?";
        try (PreparedStatement duplicateCheckStatement = connection.prepareStatement(duplicateCheckQuery)) {
            duplicateCheckStatement.setString(1, code);
            duplicateCheckStatement.setInt(2, id);
            ResultSet rs = duplicateCheckStatement.executeQuery();
            
            if (rs.next()) {
                sendErrorResponse(response, "Discount code already exists", 400);
                return;
            }
        }
        
        // Update discount code
        String query = "UPDATE discount_codes SET code = ?, discount_value = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, code);
            statement.setDouble(2, discountValue);
            statement.setInt(3, id);
            
            int rowsAffected = statement.executeUpdate();
            
            if (rowsAffected > 0) {
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("message", "Discount code updated successfully");
                
                sendSuccessResponse(response, result.toString());
            } else {
                sendErrorResponse(response, "Failed to update discount code", 500);
            }
        }
    }

    private void deleteDiscountCode(int id, HttpServletResponse response) throws Exception {
        Connection connection = dbConnection.connect();
        String query = "DELETE FROM discount_codes WHERE id = ?";
        
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            
            int rowsAffected = statement.executeUpdate();
            
            if (rowsAffected > 0) {
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("message", "Discount code deleted successfully");
                
                sendSuccessResponse(response, result.toString());
            } else {
                sendErrorResponse(response, "Discount code not found", 404);
            }
        }
    }

    private void sendSuccessResponse(HttpServletResponse response, String data) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        
        response.getWriter().write(data);
    }

    private void sendErrorResponse(HttpServletResponse response, String message, int statusCode) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        
        JSONObject error = new JSONObject();
        error.put("success", false);
        error.put("error", message);
        
        response.getWriter().write(error.toString());
    }
}
