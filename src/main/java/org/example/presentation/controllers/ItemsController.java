package org.example.presentation.controllers;

import org.example.persistence.database.DatabaseConnection;
import org.json.JSONArray;
import org.json.JSONObject;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ItemsController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String pathInfo = request.getPathInfo();
        String query = request.getParameter("query");
        
        try {
            if (pathInfo != null && pathInfo.equals("/search")) {
                handleSearch(query, response);
            } else if (pathInfo != null && pathInfo.length() > 1) {
                // Get specific item by code
                String itemCode = pathInfo.substring(1); // Remove leading slash
                handleGetItem(itemCode, response);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\":\"Invalid request\"}");
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Internal server error\"}");
        }
    }
    
    private void handleSearch(String query, HttpServletResponse response) throws IOException {
        if (query == null || query.trim().isEmpty()) {
            response.getWriter().write("[]");
            return;
        }
        
        JSONArray results = new JSONArray();
        DatabaseConnection dbConnection = DatabaseConnection.getInstance();
        Connection conn = null;
        
        try {
            conn = dbConnection.connect();
            String sql = "SELECT UPPER(code) as code, name, price, " +
                        "CASE " +
                        "    WHEN name LIKE '%rice%' OR name LIKE '%Rice%' THEN 'Grains' " +
                        "    WHEN name LIKE '%oil%' OR name LIKE '%Oil%' THEN 'Cooking Oils' " +
                        "    WHEN name LIKE '%sugar%' OR name LIKE '%Sugar%' THEN 'Sweeteners' " +
                        "    ELSE 'General' " +
                        "END as category, " +
                        "name as description " +
                        "FROM items " +
                        "WHERE UPPER(code) LIKE ? OR UPPER(name) LIKE ? " +
                        "ORDER BY code LIMIT 10";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                String searchPattern = "%" + query.trim().toUpperCase() + "%";
                stmt.setString(1, searchPattern);
                stmt.setString(2, searchPattern);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        JSONObject item = new JSONObject();
                        item.put("code", rs.getString("code"));
                        item.put("name", rs.getString("name"));
                        item.put("price", rs.getDouble("price"));
                        item.put("category", rs.getString("category"));
                        results.put(item);
                    }
                }
            }
        } catch (Exception e) {
            // Return empty array on error
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                }
            }
        }
        
        response.getWriter().write(results.toString());
    }
    
    private void handleGetItem(String itemCode, HttpServletResponse response) throws IOException {
        DatabaseConnection dbConnection = DatabaseConnection.getInstance();
        Connection conn = null;
        
        try {
            conn = dbConnection.connect();
            String sql = "SELECT * FROM items WHERE UPPER(code) = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, itemCode.toUpperCase());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        JSONObject item = new JSONObject();
                        item.put("code", rs.getString("code"));
                        item.put("name", rs.getString("name"));
                        item.put("price", rs.getDouble("price"));
                        item.put("category", "General"); // Default category
                        item.put("description", rs.getString("name"));
                        
                        response.getWriter().write(item.toString());
                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        response.getWriter().write("{\"error\":\"Item not found\"}");
                    }
                }
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Internal server error\"}");
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                }
            }
        }
    }
}
