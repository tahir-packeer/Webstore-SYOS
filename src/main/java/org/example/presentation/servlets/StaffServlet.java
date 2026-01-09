package org.example.presentation.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.persistence.database.DatabaseConnection;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class StaffServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        // Handle CORS
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");

        String pathInfo = req.getPathInfo();
        
        if (pathInfo != null && pathInfo.equals("/login")) {
            handleStaffLogin(req, resp);
        } else {
            resp.setStatus(404);
            resp.getWriter().write("{\"error\":\"Endpoint not found\"}");
        }
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Handle CORS preflight
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
        resp.setStatus(200);
    }

    private void handleStaffLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            // Read JSON from request body
            StringBuilder sb = new StringBuilder();
            String line;
            try (BufferedReader reader = req.getReader()) {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            JSONObject loginData = new JSONObject(sb.toString());
            String username = loginData.getString("username");
            String password = loginData.getString("password");
            String role = loginData.optString("role", ""); // Role is optional for validation

            System.out.println("Staff login attempt - Username: " + username + ", Role: " + role);

            // Authenticate staff member
            DatabaseConnection db = DatabaseConnection.getInstance();
            Connection connection = db.connect();

            String query = "SELECT id, name, type FROM users WHERE name = ? AND password = ?";
            
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, username);
                statement.setString(2, password);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        int userId = resultSet.getInt("id");
                        String name = resultSet.getString("name");
                        String userType = resultSet.getString("type");

                        System.out.println("Staff member found: " + name + " (Type: " + userType + ")");

                        // Check if provided role matches (if role was specified)
                        if (!role.isEmpty() && !role.equalsIgnoreCase(userType)) {
                            System.out.println("Role mismatch: expected " + role + ", got " + userType);
                            resp.setStatus(401);
                            resp.getWriter().write("{\"error\":\"Invalid role for this user\"}");
                            return;
                        }

                        // Return user data
                        JSONObject response = new JSONObject();
                        response.put("id", userId);
                        response.put("name", name);
                        response.put("type", userType);
                        response.put("success", true);

                        resp.setStatus(200);
                        resp.getWriter().write(response.toString());
                        
                    } else {
                        System.out.println("Authentication failed for username: " + username);
                        resp.setStatus(401);
                        resp.getWriter().write("{\"error\":\"Invalid username or password\"}");
                    }
                }
            } finally {
                connection.close();
            }

        } catch (Exception e) {
            System.err.println("Staff login error: " + e.getMessage());
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\"Internal server error: " + e.getMessage() + "\"}");
        }
    }
}
