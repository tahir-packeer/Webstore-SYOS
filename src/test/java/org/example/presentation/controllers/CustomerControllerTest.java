package org.example.presentation.controllers;

import org.example.persistence.database.DatabaseConnection;
import org.example.persistence.models.Customer;
import org.junit.jupiter.api.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("CustomerController Test Suite")
class CustomerControllerTest {

    private CustomerController customerController;
    private DatabaseConnection databaseConnection;

    @BeforeEach
    @DisplayName("Setup test environment")
    void setUp() throws Exception {
        customerController = new CustomerController();
        databaseConnection = DatabaseConnection.getInstance();
        databaseConnection.connect();

        // Clean up any existing test data
        cleanupTestData();
    }

    private void cleanupTestData() {
        try {
            String[] testPhones = {
                    "0771234567", "0772345678", "0773456789", "0774444444",
                    "0776789012", "0773333333", "0770000000", "0771111111", "0772222222"
            };

            Connection connection = databaseConnection.connect();
            for (String phone : testPhones) {
                try (PreparedStatement stmt = connection.prepareStatement(
                        "DELETE FROM customers WHERE contactNumber = ?")) {
                    stmt.setString(1, phone);
                    stmt.executeUpdate();
                }
            }
            databaseConnection.closeConnection(connection);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should add customer with valid details successfully")
    void addCustomer_ValidDetails_Success() throws Exception {
        // Arrange
        Customer testCustomer = new Customer("Alice Johnson", "0771234567");

        // Act & Assert
        assertDoesNotThrow(() -> {
            customerController.add_Customer(testCustomer);
        });

        // Verify customer was added
        Customer retrievedCustomer = customerController.get_Customer_from_contactNumber("0771234567");
        assertNotNull(retrievedCustomer);
        assertEquals("Alice Johnson", retrievedCustomer.getName());
        assertEquals("0771234567", retrievedCustomer.getcontactNumber());
        assertTrue(retrievedCustomer.getId() > 0);
    }

    @Test
    @Order(2)
    @DisplayName("Should retrieve customer by contact number successfully")
    void getCustomerFromContactNumber_ValidNumber_ReturnsCustomer() throws Exception {
        // Arrange
        Customer testCustomer = new Customer("Bob Smith", "0772345678");
        customerController.add_Customer(testCustomer);

        // Act
        Customer retrievedCustomer = customerController.get_Customer_from_contactNumber("0772345678");

        // Assert
        assertNotNull(retrievedCustomer);
        assertEquals("Bob Smith", retrievedCustomer.getName());
        assertEquals("0772345678", retrievedCustomer.getcontactNumber());
        assertTrue(retrievedCustomer.getId() > 0);
    }

    @Test
    @Order(3)
    @DisplayName("Should return null for non-existent contact number")
    void getCustomerFromContactNumber_NonExistentNumber_ReturnsNull() throws Exception {
        // Act
        Customer nonExistentCustomer = customerController.get_Customer_from_contactNumber("0999999999");

        // Assert
        assertNull(nonExistentCustomer);
    }

    @Test
    @Order(4)
    @DisplayName("Should handle empty contact number gracefully")
    void getCustomerFromContactNumber_EmptyNumber_HandlesGracefully() throws Exception {
        // Act & Assert
        assertDoesNotThrow(() -> {
            Customer customer = customerController.get_Customer_from_contactNumber("");
            assertNull(customer);
        });
    }

    @Test
    @Order(5)
    @DisplayName("Should handle null contact number gracefully")
    void getCustomerFromContactNumber_NullNumber_HandlesGracefully() throws Exception {
        // Act & Assert
        assertDoesNotThrow(() -> {
            Customer customer = customerController.get_Customer_from_contactNumber(null);
            assertNull(customer);
        });
    }

    @Test
    @Order(6)
    @DisplayName("Should add customer with special characters in name")
    void addCustomer_SpecialCharactersName_Success() throws Exception {
        // Arrange
        Customer specialCharCustomer = new Customer("María José O'Connor-Smith", "0773456789");

        // Act & Assert
        assertDoesNotThrow(() -> {
            customerController.add_Customer(specialCharCustomer);
        });

        // Verify special characters are preserved
        Customer retrievedCustomer = customerController.get_Customer_from_contactNumber("0773456789");
        assertNotNull(retrievedCustomer);
        assertEquals("María José O'Connor-Smith", retrievedCustomer.getName());
    }

    @Test
    @Order(7)
    @DisplayName("Should handle duplicate customer names with different phone numbers")
    void addCustomer_DuplicateNames_DifferentPhones_Success() throws Exception {
        // Arrange
        Customer customer1 = new Customer("John Doe", "0776789012");
        Customer customer2 = new Customer("John Doe", "0777890123");

        // Act & Assert
        assertDoesNotThrow(() -> {
            customerController.add_Customer(customer1);
            customerController.add_Customer(customer2);
        });

        // Verify both customers exist with different phone numbers
        Customer retrieved1 = customerController.get_Customer_from_contactNumber("0776789012");
        Customer retrieved2 = customerController.get_Customer_from_contactNumber("0777890123");

        assertNotNull(retrieved1);
        assertNotNull(retrieved2);
        assertEquals("John Doe", retrieved1.getName());
        assertEquals("John Doe", retrieved2.getName());
        assertNotEquals(retrieved1.getId(), retrieved2.getId()); // Different IDs
    }

    @Test
    @Order(8)
    @DisplayName("Should maintain data integrity after multiple operations")
    void customerOperations_DataIntegrity_Maintained() throws Exception {
        // Arrange
        Customer originalCustomer = new Customer("Integrity Test", "0774444444");

        // Act
        customerController.add_Customer(originalCustomer);
        Customer retrievedCustomer = customerController.get_Customer_from_contactNumber("0774444444");

        // Assert - All data should match exactly
        assertNotNull(retrievedCustomer);
        assertEquals(originalCustomer.getName(), retrievedCustomer.getName());
        assertEquals(originalCustomer.getcontactNumber(), retrievedCustomer.getcontactNumber());
        assertTrue(retrievedCustomer.getId() > 0); // Should have database-generated ID

        // Retrieve again to ensure consistency
        Customer retrievedAgain = customerController.get_Customer_from_contactNumber("0774444444");
        assertNotNull(retrievedAgain);
        assertEquals(retrievedCustomer.getId(), retrievedAgain.getId()); // Same ID
        assertEquals(retrievedCustomer.getName(), retrievedAgain.getName()); // Same name
        assertEquals(retrievedCustomer.getcontactNumber(), retrievedAgain.getcontactNumber()); // Same contact
    }
}
