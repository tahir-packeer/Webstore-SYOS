
package org.example.presentation.servlets;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.BufferedReader;
import java.sql.*;

import org.example.presentation.controllers.OnlineController;
import org.example.persistence.models.Customer;
import org.example.persistence.database.DatabaseConnection;

public class CustomerServlet extends HttpServlet {
    
    // Handles customer registration and authentication
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            BufferedReader reader = req.getReader();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String json = sb.toString();
            JSONObject requestData = new JSONObject(json);
            
            // Check if this is a login request (has email and password but no name)
            if (requestData.has("email") && requestData.has("password") && !requestData.has("name")) {
                handleLogin(requestData, resp);
            } else {
                handleRegistration(requestData, resp);
            }
            
        } catch (Exception e) {
            resp.setStatus(400);
            JSONObject error = new JSONObject();
            error.put("error", "Invalid request: " + e.getMessage());
            resp.getWriter().write(error.toString());
        }
    }
    
    private void handleLogin(JSONObject requestData, HttpServletResponse resp) throws IOException {
        try {
            String email = requestData.getString("email");
            String password = requestData.getString("password");
            
            Customer customer = authenticateCustomer(email, password);
            
            if (customer != null) {
                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("id", customer.getId());
                response.put("name", customer.getName());
                response.put("email", customer.getEmail());
                response.put("contactNumber", customer.getcontactNumber());
                response.put("address", customer.getAddress());
                resp.getWriter().write(response.toString());
            } else {
                resp.setStatus(401);
                JSONObject error = new JSONObject();
                error.put("error", "Invalid email or password");
                resp.getWriter().write(error.toString());
            }
            
        } catch (Exception e) {
            resp.setStatus(500);
            JSONObject error = new JSONObject();
            error.put("error", "Login failed: " + e.getMessage());
            resp.getWriter().write(error.toString());
        }
    }
    
    private void handleRegistration(JSONObject requestData, HttpServletResponse resp) throws IOException {
        try {
            String name = requestData.getString("name");
            String contactNumber = requestData.getString("contactNumber");
            String email = requestData.optString("email", "");
            String address = requestData.optString("address", "");
            String password = requestData.optString("password", "");
            
            if (password.isEmpty()) {
                resp.setStatus(400);
                JSONObject error = new JSONObject();
                error.put("error", "Password is required");
                resp.getWriter().write(error.toString());
                return;
            }
            
            // Register customer in the database
            int customerId = registerCustomer(name, contactNumber, email, address, password);
            
            if (customerId > 0) {
                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("id", customerId);
                response.put("message", "Customer registered successfully");
                resp.getWriter().write(response.toString());
            } else {
                resp.setStatus(400);
                JSONObject error = new JSONObject();
                error.put("error", "Registration failed. Email or contact number may already exist.");
                resp.getWriter().write(error.toString());
            }
            
        } catch (Exception e) {
            resp.setStatus(500);
            JSONObject error = new JSONObject();
            error.put("error", "Registration failed: " + e.getMessage());
            resp.getWriter().write(error.toString());
        }
    }
    
    private Customer authenticateCustomer(String email, String password) throws SQLException, ClassNotFoundException {
        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();
        
        try {
            // Authenticate online customers from online_customers table
            String query = "SELECT * FROM online_customers WHERE email = ? AND password = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, email);
                statement.setString(2, password);
                
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        Customer customer = new Customer(
                            rs.getString("name"),
                            rs.getString("contactNumber"),
                            rs.getString("email"),
                            rs.getString("address")
                        );
                        customer.setId(rs.getInt("id"));
                        System.out.println("Online customer authenticated: " + customer.getName() + " (ID: " + customer.getId() + ")");
                        return customer;
                    }
                }
            }
        } finally {
            connection.close();
        }
        return null;
    }
    
    private int registerCustomer(String name, String contactNumber, String email, String address, String password) 
            throws SQLException, ClassNotFoundException {
        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();
        
        try {
            // Register in online_customers table for e-commerce customers
            String query = "INSERT INTO online_customers (name, contactNumber, email, address, password, registrationDate) VALUES (?, ?, ?, ?, ?, NOW())";
            try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, name);
                statement.setString(2, contactNumber);
                statement.setString(3, email);
                statement.setString(4, address);
                statement.setString(5, password);
                
                int rowsAffected = statement.executeUpdate();
                if (rowsAffected > 0) {
                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            System.out.println("Online customer registered successfully: " + name + " (ID: " + generatedKeys.getInt(1) + ")");
                            return generatedKeys.getInt(1);
                        }
                    }
                }
            }
        } finally {
            connection.close();
        }
        return -1;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        String contactNumberParam = req.getParameter("id");
        try {
            OnlineController onlineController = new OnlineController();
            if (contactNumberParam != null) {
                // Fetch single online customer by contact number
                Customer customer = onlineController.getOnlineCustomerByContactNumber(contactNumberParam);
                if (customer == null) {
                    resp.setStatus(404);
                    resp.getWriter().write("{\"error\":\"Online customer not found\"}");
                    return;
                }
                JSONObject obj = new JSONObject();
                obj.put("id", customer.getId());
                obj.put("name", customer.getName());
                obj.put("contactNumber", customer.getcontactNumber());
                obj.put("email", customer.getEmail());
                obj.put("address", customer.getAddress());
                resp.getWriter().write(obj.toString());
            } else {
                // List all online customers
                java.util.List<Customer> onlineCustomers = onlineController.getAllOnlineCustomers();
                JSONArray arr = new JSONArray();
                for (Customer customer : onlineCustomers) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", customer.getId());
                    obj.put("name", customer.getName());
                    obj.put("contactNumber", customer.getcontactNumber());
                    obj.put("email", customer.getEmail());
                    obj.put("address", customer.getAddress());
                    arr.put(obj);
                }
                resp.getWriter().write(arr.toString());
            }
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
