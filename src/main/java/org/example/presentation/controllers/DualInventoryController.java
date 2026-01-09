package org.example.presentation.controllers;

import org.example.business.services.DualInventoryService;
import org.example.business.services.DualInventoryService.ShelfInventory;
import org.json.JSONObject;
import org.json.JSONArray;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * SYOS Dual Inventory API Controller
 * Handles STORE vs WEBSITE shelf management, transfers, and availability checking
 */
@WebServlet("/api/inventory/*")
public class DualInventoryController extends HttpServlet {

    private DualInventoryService inventoryService;

    @Override
    public void init() throws ServletException {
        super.init();
        this.inventoryService = DualInventoryService.getInstance();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        setCommonHeaders(response);
        String pathInfo = request.getPathInfo();
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                handleGetInventoryOverview(request, response);
            } else if (pathInfo.equals("/overview")) {
                handleGetInventoryOverview(request, response);
            } else {
                sendErrorResponse(response, 404, "Endpoint not found");
            }
        } catch (Exception e) {
            sendErrorResponse(response, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handleGetInventoryOverview(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        JSONObject result = new JSONObject();
        
        try {
            List<ShelfInventory> dualInventory = inventoryService.getDualInventoryOverview();
            JSONArray storeArray = new JSONArray();
            JSONArray websiteArray = new JSONArray();
            
            for (ShelfInventory item : dualInventory) {
                // Store shelf data
                JSONObject storeItemObj = new JSONObject();
                storeItemObj.put("itemCode", item.getItemCode());
                storeItemObj.put("itemName", item.getItemName());
                storeItemObj.put("quantity", item.getStoreQuantity());
                storeArray.put(storeItemObj);
                
                // Website shelf data
                JSONObject websiteItemObj = new JSONObject();
                websiteItemObj.put("itemCode", item.getItemCode());
                websiteItemObj.put("itemName", item.getItemName());
                websiteItemObj.put("quantity", item.getWebsiteQuantity());
                websiteArray.put(websiteItemObj);
            }
            
            result.put("success", true);
            result.put("storeShelf", storeArray);
            result.put("websiteShelf", websiteArray);
            result.put("timestamp", new Date().toString());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        sendJsonResponse(response, result);
    }

    private void setCommonHeaders(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void sendJsonResponse(HttpServletResponse response, JSONObject json) throws IOException {
        PrintWriter out = response.getWriter();
        out.print(json.toString());
        out.flush();
    }

    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        JSONObject error = new JSONObject();
        error.put("success", false);
        error.put("error", message);
        error.put("timestamp", new Date().toString());
        sendJsonResponse(response, error);
    }
}
