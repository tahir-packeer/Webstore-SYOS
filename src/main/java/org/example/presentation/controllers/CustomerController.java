package org.example.presentation.controllers;

import org.example.persistence.database.DatabaseConnection;
import org.example.persistence.models.Customer;
import org.example.business.validators.CustomerValidator;

import java.sql.Connection;
import java.sql.SQLException;

public class CustomerController {

    public Customer get_Customer_from_contactNumber(String contactNumber)
            throws SQLException, ClassNotFoundException {
        DatabaseConnection databaseConnection = DatabaseConnection.getInstance();
        Customer customer = null;
        Connection connection = databaseConnection.connect();

        try {
            var statement = connection.prepareStatement("select * from customers where contactNumber = ?");
            statement.setString(1, contactNumber);
            var resultSet = statement.executeQuery();
            if (resultSet.next()) {
                String customerName = resultSet.getString("name");
                String customerContact = resultSet.getString("contactNumber");

                customer = new Customer(customerName, customerContact);
                customer.setId(resultSet.getInt("id"));

                return customer;
            } else {
                System.out.println("Customer with contact number " + contactNumber + " not found.");

                return customer;
            }
        } finally {
            databaseConnection.closeConnection(connection);
        }
    }

    public Customer get_OnlineCustomer_from_contactNumber(String contactNumber)
            throws SQLException, ClassNotFoundException {
        DatabaseConnection databaseConnection = DatabaseConnection.getInstance();
        Customer customer = null;
        Connection connection = databaseConnection.connect();

        try {
            var statement = connection.prepareStatement("select * from online_customers where contactNumber = ?");
            statement.setString(1, contactNumber);
            var resultSet = statement.executeQuery();
            if (resultSet.next()) {
                String customerName = resultSet.getString("name");
                String customerContact = resultSet.getString("contactNumber");

                customer = new Customer(customerName, customerContact);
                customer.setId(resultSet.getInt("id")); // Set the database ID

                return customer;
            } else {
                System.out.println("Customer with contact number " + contactNumber + " not found.");

                return customer;
            }
        } finally {
            databaseConnection.closeConnection(connection);
        }
    }

    public void add_Customer(Customer customer)
            throws SQLException, ClassNotFoundException {
        // Validate customer data before processing
        CustomerValidator.ValidationResult validation = CustomerValidator.validateCustomerData(
                customer.getName(),
                customer.getcontactNumber(),
                null, // email not required for in-store customers
                null // address not required for in-store customers
        );

        if (!validation.isValid()) {
            System.out.println("Customer validation failed: " + validation.getErrors());
            throw new IllegalArgumentException("Invalid customer data: " + validation.getErrors());
        }

        DatabaseConnection databaseConnection = DatabaseConnection.getInstance();
        Connection connection = databaseConnection.connect();

        try {
            var statement = connection.prepareStatement("insert into customers(name, contactNumber) values(?, ?)");
            statement.setString(1, customer.getName().trim());
            statement.setString(2, customer.getcontactNumber().trim());

            int rowsInserted = statement.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("Customer added successfully: " + customer.getName());
            } else {
                // Failed to add customer
            }
        } finally {
            databaseConnection.closeConnection(connection);
        }
    }
}
