
package org.example.presentation.servlets;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.BufferedReader;
import java.sql.Connection;
import java.sql.Date;

import org.example.presentation.controllers.ItemController;
import org.example.presentation.controllers.StockController;
import org.example.persistence.models.Item;
import org.example.persistence.models.Stock;

public class StockServlet extends HttpServlet {
    // Handles stock management operations
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        try {
            Connection conn = org.example.persistence.database.DatabaseConnection.getInstance().connect();
            System.out.println("Database connection established successfully");
            
            // Check if this is a request for website shelf items
            String type = req.getParameter("type");
            boolean isWebsiteRequest = "website".equals(type);
            
            java.sql.PreparedStatement ps;
            if (isWebsiteRequest) {
                // Query shelf table for WEBSITE items (for customer catalog)
                ps = conn.prepareStatement(
                    "SELECT s.id, i.code, i.name, i.price, s.quantity, NULL as date_of_purchase, NULL as date_of_expiry, (s.quantity > 0) as availability FROM shelf s JOIN items i ON s.item_id = i.id WHERE s.type = 'WEBSITE' AND s.quantity > 0 ORDER BY i.name ASC");
                System.out.println("Executing query to fetch website shelf items...");
            } else {
                // Default: Query stock table for warehouse inventory
                ps = conn.prepareStatement(
                    "SELECT s.id, i.code, i.name, i.price, s.quantity, s.date_of_purchase, s.date_of_expiry, s.availability FROM stock s JOIN items i ON s.item_id = i.id ORDER BY s.date_of_expiry ASC, s.date_of_purchase ASC");
                System.out.println("Executing query to fetch stock items...");
            }
            
            java.sql.ResultSet rs = ps.executeQuery();
            System.out.println("Query executed, processing results...");
            
            JSONArray arr = new JSONArray();
            int count = 0;
            while (rs.next()) {
                JSONObject obj = new JSONObject();
                obj.put("id", rs.getInt("id"));
                obj.put("code", rs.getString("code"));
                obj.put("name", rs.getString("name"));
                obj.put("price", rs.getDouble("price"));
                obj.put("quantity", rs.getInt("quantity"));
                
                // Handle nullable date fields for website shelf items
                if (!isWebsiteRequest) {
                    obj.put("date_of_purchase", rs.getDate("date_of_purchase"));
                    obj.put("date_of_expiry", rs.getDate("date_of_expiry"));
                }
                obj.put("availability", rs.getBoolean("availability"));
                arr.put(obj);
                count++;
            }
            System.out.println("Found " + count + (isWebsiteRequest ? " website shelf items" : " stock items"));
            
            ps.close();
            rs.close();
            conn.close();
            
            resp.getWriter().write(arr.toString());
        } catch (Exception e) {
            System.err.println("Error in StockServlet doGet: " + e.getMessage());
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

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
            String code = obj.getString("code");
            int quantity = obj.getInt("quantity");
            String purchaseDate = obj.getString("date_of_purchase");
            String expiryDate = obj.getString("date_of_expiry");

            ItemController itemController = new ItemController();
            StockController stockController = new StockController();
            Item item = itemController.getItemFromCode(code);
            if (item == null) {
                resp.setStatus(400);
                resp.getWriter().write("{\"error\":\"Item code not found\"}");
                return;
            }
            Stock stock = new Stock(item, quantity, Date.valueOf(expiryDate));
            stock.setDate_of_purchase(Date.valueOf(purchaseDate));
            stock.setAvailability(true);
            stockController.add_items_to_stock(stock);
            resp.getWriter().write("{\"success\":true}");
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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
            int stockId = obj.getInt("stockId");
            int shelfId = obj.getInt("shelfId");
            int quantityMoved = obj.getInt("quantityMoved");

            try (Connection conn = org.example.persistence.database.DatabaseConnection.getInstance().connect()) {
                // Deduct from stock
                java.sql.PreparedStatement upd = conn.prepareStatement("UPDATE stock SET quantity = quantity - ? WHERE id = ? AND quantity >= ?");
                upd.setInt(1, quantityMoved);
                upd.setInt(2, stockId);
                upd.setInt(3, quantityMoved);
                int affected = upd.executeUpdate();
                upd.close();
                if (affected == 0) {
                    resp.setStatus(400);
                    resp.getWriter().write("{\"error\":\"Insufficient stock or invalid stockId\"}");
                    return;
                }
                // Add to shelf_stock tracking
                java.sql.PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO shelf_stock (stock_id, shelf_id, quantity_moved) VALUES (?, ?, ?)");
                ins.setInt(1, stockId);
                ins.setInt(2, shelfId);
                ins.setInt(3, quantityMoved);
                ins.executeUpdate();
                ins.close();
                // Update shelf quantity
                java.sql.PreparedStatement shelfUpd = conn.prepareStatement("UPDATE shelf SET quantity = quantity + ? WHERE id = ?");
                shelfUpd.setInt(1, quantityMoved);
                shelfUpd.setInt(2, shelfId);
                shelfUpd.executeUpdate();
                shelfUpd.close();
                resp.getWriter().write("{\"success\":true}");
            }
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        String stockIdParam = req.getParameter("stockId");
        if (stockIdParam == null) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"Missing stockId parameter\"}");
            return;
        }
        try (Connection conn = org.example.persistence.database.DatabaseConnection.getInstance().connect()) {
            int stockId = Integer.parseInt(stockIdParam);
            java.sql.PreparedStatement del = conn.prepareStatement("DELETE FROM stock WHERE id = ?");
            del.setInt(1, stockId);
            int affected = del.executeUpdate();
            del.close();
            if (affected == 0) {
                resp.setStatus(404);
                resp.getWriter().write("{\"error\":\"Stock batch not found\"}");
            } else {
                resp.getWriter().write("{\"success\":true}");
            }
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
