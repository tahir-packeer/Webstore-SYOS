package org.example.presentation.servlets;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.BufferedReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.example.presentation.controllers.CustomerController;
import org.example.persistence.models.Customer;
import org.example.persistence.database.DatabaseConnection;

/**
 * Servlet for handling regular store customers (POS system)
 * Separate from online customers to handle different requirements
 */
public class POSCustomerServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();

        try {
            if ("/register".equals(pathInfo)) {
                handleCustomerRegistration(req, resp);
            } else {
                resp.setStatus(404);
                JSONObject error = new JSONObject();
                error.put("error", "Endpoint not found");
                resp.getWriter().write(error.toString());
            }
        } catch (Exception e) {
            resp.setStatus(500);
            JSONObject error = new JSONObject();
            error.put("error", "Server error: " + e.getMessage());
            resp.getWriter().write(error.toString());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();

        try {
            if ("/search".equals(pathInfo)) {
                handleCustomerSearch(req, resp);
            } else {
                resp.setStatus(404);
                JSONObject error = new JSONObject();
                error.put("error", "Endpoint not found");
                resp.getWriter().write(error.toString());
            }
        } catch (Exception e) {
            resp.setStatus(500);
            JSONObject error = new JSONObject();
            error.put("error", "Server error: " + e.getMessage());
            resp.getWriter().write(error.toString());
        }
    }

    private void handleCustomerRegistration(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            BufferedReader reader = req.getReader();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String json = sb.toString();
            JSONObject requestData = new JSONObject(json);

            String name = requestData.getString("name").trim();
            String contactNumber = requestData.getString("contactNumber").trim();

            if (name.isEmpty() || contactNumber.isEmpty()) {
                resp.setStatus(400);
                JSONObject error = new JSONObject();
                error.put("success", false);
                error.put("message", "Name and contact number are required");
                resp.getWriter().write(error.toString());
                return;
            }

            // Validate phone number format (basic validation)
            String cleanPhone = contactNumber.replaceAll("\\D", "");
            if (cleanPhone.length() != 10) {
                resp.setStatus(400);
                JSONObject error = new JSONObject();
                error.put("success", false);
                error.put("message", "Please enter a valid 10-digit phone number");
                resp.getWriter().write(error.toString());
                return;
            }

            // Check if customer already exists
            CustomerController customerController = new CustomerController();
            Customer existingCustomer = customerController.get_Customer_from_contactNumber(contactNumber);

            if (existingCustomer != null) {
                resp.setStatus(409); // Conflict
                JSONObject error = new JSONObject();
                error.put("success", false);
                error.put("message", "A customer with this phone number already exists");
                resp.getWriter().write(error.toString());
                return;
            }

            // Register new customer
            Customer newCustomer = new Customer(name, contactNumber);
            customerController.add_Customer(newCustomer);

            // Get the newly created customer to return the ID
            Customer createdCustomer = customerController.get_Customer_from_contactNumber(contactNumber);

            JSONObject response = new JSONObject();
            response.put("success", true);
            response.put("customerId", createdCustomer.getId());
            response.put("message", "Customer registered successfully");
            resp.getWriter().write(response.toString());

        } catch (Exception e) {
            resp.setStatus(500);
            JSONObject error = new JSONObject();
            error.put("success", false);
            error.put("message", "Registration failed: " + e.getMessage());
            resp.getWriter().write(error.toString());
        }
    }

    private void handleCustomerSearch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String searchTerm = req.getParameter("term");

            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                JSONObject response = new JSONObject();
                response.put("customers", new JSONArray());
                resp.getWriter().write(response.toString());
                return;
            }

            searchTerm = searchTerm.trim();

            // Basic input validation to prevent malicious input
            if (searchTerm.length() > 100) {
                JSONObject error = new JSONObject();
                error.put("error", "Search term too long");
                resp.setStatus(400);
                resp.getWriter().write(error.toString());
                return;
            }

            List<Customer> customers = searchCustomers(searchTerm);

            JSONArray customerArray = new JSONArray();
            for (Customer customer : customers) {
                JSONObject customerObj = new JSONObject();
                customerObj.put("id", customer.getId());
                customerObj.put("name", customer.getName());
                customerObj.put("contactNumber", customer.getcontactNumber());
                customerArray.put(customerObj);
            }

            JSONObject response = new JSONObject();
            response.put("customers", customerArray);
            resp.getWriter().write(response.toString());

        } catch (Exception e) {
            resp.setStatus(500);
            JSONObject error = new JSONObject();
            error.put("error", "Search failed: " + e.getMessage());
            resp.getWriter().write(error.toString());
        }
    }

    private List<Customer> searchCustomers(String searchTerm) throws SQLException, ClassNotFoundException {
        List<Customer> customers = new ArrayList<>();
        DatabaseConnection databaseConnection = DatabaseConnection.getInstance();
        Connection connection = databaseConnection.connect();

        try {
            // Search by name or contact number
            String query = "SELECT * FROM customers WHERE name LIKE ? OR contactNumber LIKE ? ORDER BY name LIMIT 10";
            PreparedStatement statement = connection.prepareStatement(query);
            String searchPattern = "%" + searchTerm + "%";
            statement.setString(1, searchPattern);
            statement.setString(2, searchPattern);

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                Customer customer = new Customer(
                        resultSet.getString("name"),
                        resultSet.getString("contactNumber"));
                customer.setId(resultSet.getInt("id"));
                customers.add(customer);
            }

        } finally {
            databaseConnection.closeConnection(connection);
        }

        return customers;
    }
}