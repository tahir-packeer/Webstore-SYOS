package org.example.presentation.servlets;

import org.example.persistence.database.DatabaseConnection;
import org.json.JSONObject;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.BufferedReader;
import java.sql.*;
import java.util.*;

@WebServlet(name = "StoreManagerServlet", urlPatterns = {"/api/store-manager/*"})
public class StoreManagerServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String pathInfo = request.getPathInfo();
        
        try {
            if (pathInfo == null) {
                sendErrorResponse(response, "Invalid endpoint");
                return;
            }
            
            switch (pathInfo) {
                case "/listItems":
                    listItems(response);
                    break;
                case "/listStock":
                    listStock(response);
                    break;
                case "/listShelf":
                    listShelf(response);
                    break;
                case "/listDiscountCodes":
                    listDiscountCodes(response);
                    break;
                case "/getItem":
                    getItem(request, response);
                    break;
                case "/getDiscountCode":
                    getDiscountCode(request, response);
                    break;
                default:
                    sendErrorResponse(response, "Endpoint not found");
            }
        } catch (Exception e) {
            sendErrorResponse(response, "Internal server error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String pathInfo = request.getPathInfo();
        
        try {
            if (pathInfo == null) {
                sendErrorResponse(response, "Invalid endpoint");
                return;
            }
            
            switch (pathInfo) {
                case "/reshelveItems":
                    reshelveItems(request, response);
                    break;
                case "/moveOnlineToStore":
                    moveOnlineToStore(request, response);
                    break;
                case "/updateItem":
                    updateItem(request, response);
                    break;
                case "/addDiscountCode":
                    addDiscountCode(request, response);
                    break;
                case "/updateDiscountCode":
                    updateDiscountCode(request, response);
                    break;
                case "/deleteDiscountCode":
                    deleteDiscountCode(request, response);
                    break;
                default:
                    sendErrorResponse(response, "Endpoint not found");
            }
        } catch (Exception e) {
            sendErrorResponse(response, "Internal server error: " + e.getMessage());
        }
    }

    private void listItems(HttpServletResponse response) throws Exception {
        List<Map<String, Object>> items = new ArrayList<>();
        Connection conn = null;
        
        try {
            conn = DatabaseConnection.getInstance().connect();
            String query = """
                SELECT i.id, i.code, i.name, i.price,
                       COALESCE(SUM(CASE WHEN sh.type = 'STORE' THEN sh.quantity ELSE 0 END), 0) as store_quantity,
                       COALESCE(SUM(CASE WHEN sh.type = 'WEBSITE' THEN sh.quantity ELSE 0 END), 0) as website_quantity
                FROM items i
                LEFT JOIN shelf sh ON i.id = sh.item_id
                GROUP BY i.id, i.code, i.name, i.price
                ORDER BY i.name
                """;
            
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", rs.getInt("id"));
                item.put("code", rs.getString("code"));
                item.put("name", rs.getString("name"));
                item.put("price", rs.getDouble("price"));
                item.put("store_quantity", rs.getInt("store_quantity"));
                item.put("website_quantity", rs.getInt("website_quantity"));
                items.add(item);
            }
            
            sendSuccessResponse(response, items);
        } finally {
            if (conn != null) conn.close();
        }
    }

    private void listStock(HttpServletResponse response) throws Exception {
        List<Map<String, Object>> stockItems = new ArrayList<>();
        Connection conn = null;
        
        try {
            conn = DatabaseConnection.getInstance().connect();
            String query = """
                SELECT s.id, i.name, i.code, s.quantity
                FROM stock s
                JOIN items i ON s.item_id = i.id
                WHERE s.quantity > 0
                ORDER BY i.name
                """;
            
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> stock = new HashMap<>();
                stock.put("id", rs.getInt("id"));
                stock.put("name", rs.getString("name"));
                stock.put("code", rs.getString("code"));
                stock.put("quantity", rs.getInt("quantity"));
                stockItems.add(stock);
            }
            
            sendSuccessResponse(response, stockItems);
        } finally {
            if (conn != null) conn.close();
        }
    }

    private void listShelf(HttpServletResponse response) throws Exception {
        List<Map<String, Object>> shelfItems = new ArrayList<>();
        Connection conn = null;
        
        try {
            conn = DatabaseConnection.getInstance().connect();
            String query = """
                SELECT s.id, i.id as item_id, i.name, i.code, s.quantity, s.type as shelf_type
                FROM shelf s
                JOIN items i ON s.item_id = i.id
                WHERE s.quantity > 0
                ORDER BY i.name, s.type
                """;
            
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> shelf = new HashMap<>();
                shelf.put("id", rs.getInt("id"));
                shelf.put("item_id", rs.getInt("item_id"));
                shelf.put("name", rs.getString("name"));
                shelf.put("code", rs.getString("code"));
                shelf.put("quantity", rs.getInt("quantity"));
                shelf.put("shelf_type", rs.getString("shelf_type"));
                shelfItems.add(shelf);
            }
            
            sendSuccessResponse(response, shelfItems);
        } finally {
            if (conn != null) conn.close();
        }
    }

    private void reshelveItems(HttpServletRequest request, HttpServletResponse response) throws Exception {
        JSONObject requestData = parseJsonRequest(request);
        
        int stockId = requestData.getInt("stock_id");
        int quantity = requestData.getInt("quantity");
        String shelfType = requestData.getString("shelf_type");
        
        Connection conn = null;
        
        try {
            conn = DatabaseConnection.getInstance().connect();
            conn.setAutoCommit(false);
            
            String getStockQuery = "SELECT item_id, quantity FROM stock WHERE id = ?";
            PreparedStatement getStockStmt = conn.prepareStatement(getStockQuery);
            getStockStmt.setInt(1, stockId);
            ResultSet stockRs = getStockStmt.executeQuery();
            
            if (!stockRs.next()) {
                throw new Exception("Stock item not found");
            }
            
            int itemId = stockRs.getInt("item_id");
            int availableQuantity = stockRs.getInt("quantity");
            
            if (quantity > availableQuantity) {
                throw new Exception("Insufficient stock quantity");
            }
            
            // Update stock quantity
            String updateStockQuery = "UPDATE stock SET quantity = quantity - ? WHERE id = ?";
            PreparedStatement updateStockStmt = conn.prepareStatement(updateStockQuery);
            updateStockStmt.setInt(1, quantity);
            updateStockStmt.setInt(2, stockId);
            updateStockStmt.executeUpdate();
            
            // Add to shelf or update existing shelf entry
            String checkShelfQuery = "SELECT id, quantity FROM shelf WHERE item_id = ? AND type = ?";
            PreparedStatement checkShelfStmt = conn.prepareStatement(checkShelfQuery);
            checkShelfStmt.setInt(1, itemId);
            checkShelfStmt.setString(2, shelfType);
            ResultSet shelfRs = checkShelfStmt.executeQuery();
            
            if (shelfRs.next()) {
                // Update existing shelf entry
                int shelfId = shelfRs.getInt("id");
                String updateShelfQuery = "UPDATE shelf SET quantity = quantity + ? WHERE id = ?";
                PreparedStatement updateShelfStmt = conn.prepareStatement(updateShelfQuery);
                updateShelfStmt.setInt(1, quantity);
                updateShelfStmt.setInt(2, shelfId);
                updateShelfStmt.executeUpdate();
            } else {
                // Insert new shelf entry
                String insertShelfQuery = "INSERT INTO shelf (item_id, type, quantity) VALUES (?, ?, ?)";
                PreparedStatement insertShelfStmt = conn.prepareStatement(insertShelfQuery);
                insertShelfStmt.setInt(1, itemId);
                insertShelfStmt.setString(2, shelfType);
                insertShelfStmt.setInt(3, quantity);
                insertShelfStmt.executeUpdate();
            }
            
            conn.commit();
            sendSuccessResponse(response, "Items moved to shelf successfully");
        } catch (Exception e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    private void moveOnlineToStore(HttpServletRequest request, HttpServletResponse response) throws Exception {
        JSONObject requestData = parseJsonRequest(request);
        
        int itemId = requestData.getInt("item_id");
        int quantity = requestData.getInt("quantity");
        
        Connection conn = null;
        
        try {
            conn = DatabaseConnection.getInstance().connect();
            conn.setAutoCommit(false);
            
            // Get website shelf quantity
            String getWebsiteQuery = "SELECT id, quantity FROM shelf WHERE item_id = ? AND type = 'WEBSITE'";
            PreparedStatement getWebsiteStmt = conn.prepareStatement(getWebsiteQuery);
            getWebsiteStmt.setInt(1, itemId);
            ResultSet websiteRs = getWebsiteStmt.executeQuery();
            
            if (!websiteRs.next()) {
                throw new Exception("Item not found on website shelf");
            }
            
            int websiteShelfId = websiteRs.getInt("id");
            int availableQuantity = websiteRs.getInt("quantity");
            
            if (quantity > availableQuantity) {
                throw new Exception("Insufficient quantity on website shelf");
            }
            
            // Update website shelf quantity
            String updateWebsiteQuery = "UPDATE shelf SET quantity = quantity - ? WHERE id = ?";
            PreparedStatement updateWebsiteStmt = conn.prepareStatement(updateWebsiteQuery);
            updateWebsiteStmt.setInt(1, quantity);
            updateWebsiteStmt.setInt(2, websiteShelfId);
            updateWebsiteStmt.executeUpdate();
            
            // Add to store shelf or update existing
            String checkStoreQuery = "SELECT id, quantity FROM shelf WHERE item_id = ? AND type = 'STORE'";
            PreparedStatement checkStoreStmt = conn.prepareStatement(checkStoreQuery);
            checkStoreStmt.setInt(1, itemId);
            ResultSet storeRs = checkStoreStmt.executeQuery();
            
            if (storeRs.next()) {
                // Update existing store shelf entry
                int storeShelfId = storeRs.getInt("id");
                String updateStoreQuery = "UPDATE shelf SET quantity = quantity + ? WHERE id = ?";
                PreparedStatement updateStoreStmt = conn.prepareStatement(updateStoreQuery);
                updateStoreStmt.setInt(1, quantity);
                updateStoreStmt.setInt(2, storeShelfId);
                updateStoreStmt.executeUpdate();
            } else {
                // Insert new store shelf entry
                String insertStoreQuery = "INSERT INTO shelf (item_id, type, quantity) VALUES (?, 'STORE', ?)";
                PreparedStatement insertStoreStmt = conn.prepareStatement(insertStoreQuery);
                insertStoreStmt.setInt(1, itemId);
                insertStoreStmt.setInt(2, quantity);
                insertStoreStmt.executeUpdate();
            }
            
            conn.commit();
            sendSuccessResponse(response, "Items moved from online to store successfully");
        } catch (Exception e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    private void updateItem(HttpServletRequest request, HttpServletResponse response) throws Exception {
        JSONObject requestData = parseJsonRequest(request);
        
        int itemId = requestData.has("item_id") && !requestData.isNull("item_id") ? 
                     requestData.getInt("item_id") : 0;
        String name = requestData.getString("name");
        double price = requestData.getDouble("price");
        int storeQuantity = requestData.has("store_quantity") ? requestData.getInt("store_quantity") : 0;
        int websiteQuantity = requestData.has("website_quantity") ? requestData.getInt("website_quantity") : 0;
        
        Connection conn = null;
        
        try {
            conn = DatabaseConnection.getInstance().connect();
            conn.setAutoCommit(false);
            
            if (itemId > 0) {
                // Update existing item
                String updateItemQuery = "UPDATE items SET name = ?, price = ? WHERE id = ?";
                PreparedStatement updateItemStmt = conn.prepareStatement(updateItemQuery);
                updateItemStmt.setString(1, name);
                updateItemStmt.setDouble(2, price);
                updateItemStmt.setInt(3, itemId);
                updateItemStmt.executeUpdate();
                
                // Update shelf quantities if provided
                updateShelfQuantity(conn, itemId, "STORE", storeQuantity);
                updateShelfQuantity(conn, itemId, "WEBSITE", websiteQuantity);
                
            } else {
                // Create new item - generate code
                String code = generateItemCode(name);
                
                String insertItemQuery = "INSERT INTO items (code, name, price) VALUES (?, ?, ?)";
                PreparedStatement insertItemStmt = conn.prepareStatement(insertItemQuery, Statement.RETURN_GENERATED_KEYS);
                insertItemStmt.setString(1, code);
                insertItemStmt.setString(2, name);
                insertItemStmt.setDouble(3, price);
                insertItemStmt.executeUpdate();
                
                ResultSet generatedKeys = insertItemStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    itemId = generatedKeys.getInt(1);
                }
                
                // Add initial shelf quantities if provided
                if (storeQuantity > 0) {
                    insertShelfQuantity(conn, itemId, "STORE", storeQuantity);
                }
                if (websiteQuantity > 0) {
                    insertShelfQuantity(conn, itemId, "WEBSITE", websiteQuantity);
                }
            }
            
            conn.commit();
            sendSuccessResponse(response, "Item saved successfully");
        } catch (Exception e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    private void listDiscountCodes(HttpServletResponse response) throws Exception {
        List<Map<String, Object>> discounts = new ArrayList<>();
        Connection conn = null;
        
        try {
            conn = DatabaseConnection.getInstance().connect();
            String query = "SELECT id, code, discount_value, created_date FROM discount_codes ORDER BY created_date DESC";
            
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> discount = new HashMap<>();
                discount.put("id", rs.getInt("id"));
                discount.put("code", rs.getString("code"));
                discount.put("discount_value", rs.getDouble("discount_value"));
                discount.put("created_date", rs.getTimestamp("created_date").toString());
                discounts.add(discount);
            }
            
            sendSuccessResponse(response, discounts);
        } finally {
            if (conn != null) conn.close();
        }
    }

    private void addDiscountCode(HttpServletRequest request, HttpServletResponse response) throws Exception {
        JSONObject requestData = parseJsonRequest(request);
        
        String code = requestData.getString("code").toUpperCase();
        double discountValue = requestData.getDouble("discount_value");
        
        Connection conn = null;
        
        try {
            conn = DatabaseConnection.getInstance().connect();
            
            String query = "INSERT INTO discount_codes (code, discount_value) VALUES (?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, code);
            stmt.setDouble(2, discountValue);
            stmt.executeUpdate();
            
            sendSuccessResponse(response, "Discount code added successfully");
        } finally {
            if (conn != null) conn.close();
        }
    }

    private void updateDiscountCode(HttpServletRequest request, HttpServletResponse response) throws Exception {
        JSONObject requestData = parseJsonRequest(request);
        
        int id = requestData.getInt("id");
        String code = requestData.getString("code").toUpperCase();
        double discountValue = requestData.getDouble("discount_value");
        
        Connection conn = null;
        
        try {
            conn = DatabaseConnection.getInstance().connect();
            
            String query = "UPDATE discount_codes SET code = ?, discount_value = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, code);
            stmt.setDouble(2, discountValue);
            stmt.setInt(3, id);
            stmt.executeUpdate();
            
            sendSuccessResponse(response, "Discount code updated successfully");
        } finally {
            if (conn != null) conn.close();
        }
    }

    private void deleteDiscountCode(HttpServletRequest request, HttpServletResponse response) throws Exception {
        JSONObject requestData = parseJsonRequest(request);
        
        int id = requestData.getInt("id");
        
        Connection conn = null;
        
        try {
            conn = DatabaseConnection.getInstance().connect();
            
            String query = "DELETE FROM discount_codes WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, id);
            stmt.executeUpdate();
            
            sendSuccessResponse(response, "Discount code deleted successfully");
        } finally {
            if (conn != null) conn.close();
        }
    }

    private void getItem(HttpServletRequest request, HttpServletResponse response) throws Exception {
        int id = Integer.parseInt(request.getParameter("id"));
        Connection conn = null;
        
        try {
            conn = DatabaseConnection.getInstance().connect();
            String query = """
                SELECT i.id, i.code, i.name, i.price,
                       COALESCE(SUM(CASE WHEN sh.type = 'STORE' THEN sh.quantity ELSE 0 END), 0) as store_quantity,
                       COALESCE(SUM(CASE WHEN sh.type = 'WEBSITE' THEN sh.quantity ELSE 0 END), 0) as website_quantity
                FROM items i
                LEFT JOIN shelf sh ON i.id = sh.item_id
                WHERE i.id = ?
                GROUP BY i.id, i.code, i.name, i.price
                """;
            
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", rs.getInt("id"));
                item.put("code", rs.getString("code"));
                item.put("name", rs.getString("name"));
                item.put("price", rs.getDouble("price"));
                item.put("store_quantity", rs.getInt("store_quantity"));
                item.put("website_quantity", rs.getInt("website_quantity"));
                sendSuccessResponse(response, item);
            } else {
                sendErrorResponse(response, "Item not found");
            }
        } finally {
            if (conn != null) conn.close();
        }
    }

    private void getDiscountCode(HttpServletRequest request, HttpServletResponse response) throws Exception {
        int id = Integer.parseInt(request.getParameter("id"));
        Connection conn = null;
        
        try {
            conn = DatabaseConnection.getInstance().connect();
            String query = "SELECT id, code, discount_value, created_date FROM discount_codes WHERE id = ?";
            
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Map<String, Object> discount = new HashMap<>();
                discount.put("id", rs.getInt("id"));
                discount.put("code", rs.getString("code"));
                discount.put("discount_value", rs.getDouble("discount_value"));
                discount.put("created_date", rs.getTimestamp("created_date").toString());
                sendSuccessResponse(response, discount);
            } else {
                sendErrorResponse(response, "Discount code not found");
            }
        } finally {
            if (conn != null) conn.close();
        }
    }

    // Helper methods
    private void updateShelfQuantity(Connection conn, int itemId, String type, int quantity) throws SQLException {
        if (quantity < 0) return;
        
        String checkQuery = "SELECT id FROM shelf WHERE item_id = ? AND type = ?";
        PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
        checkStmt.setInt(1, itemId);
        checkStmt.setString(2, type);
        ResultSet rs = checkStmt.executeQuery();
        
        if (rs.next()) {
            String updateQuery = "UPDATE shelf SET quantity = ? WHERE item_id = ? AND type = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
            updateStmt.setInt(1, quantity);
            updateStmt.setInt(2, itemId);
            updateStmt.setString(3, type);
            updateStmt.executeUpdate();
        } else if (quantity > 0) {
            insertShelfQuantity(conn, itemId, type, quantity);
        }
    }

    private void insertShelfQuantity(Connection conn, int itemId, String type, int quantity) throws SQLException {
        String insertQuery = "INSERT INTO shelf (item_id, type, quantity) VALUES (?, ?, ?)";
        PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
        insertStmt.setInt(1, itemId);
        insertStmt.setString(2, type);
        insertStmt.setInt(3, quantity);
        insertStmt.executeUpdate();
    }

    private String generateItemCode(String name) {
        String code = name.toUpperCase().replaceAll("[^A-Z]", "");
        if (code.length() > 6) {
            code = code.substring(0, 6);
        } else if (code.length() < 3) {
            code = code + "ITEM";
        }
        return code + String.format("%03d", (int)(Math.random() * 1000));
    }

    private JSONObject parseJsonRequest(HttpServletRequest request) throws IOException {
        StringBuilder requestBody = new StringBuilder();
        String line;
        
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
        }
        
        return new JSONObject(requestBody.toString());
    }

    private void sendSuccessResponse(HttpServletResponse response, Object data) throws IOException {
        JSONObject responseMap = new JSONObject();
        responseMap.put("success", true);
        responseMap.put("data", data);
        
        response.getWriter().write(responseMap.toString());
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        JSONObject responseMap = new JSONObject();
        responseMap.put("success", false);
        responseMap.put("message", message);
        
        response.getWriter().write(responseMap.toString());
    }
}
