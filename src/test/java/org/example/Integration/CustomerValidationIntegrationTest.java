package org.example.Integration;

import org.example.presentation.controllers.CustomerController;
import org.example.presentation.controllers.OnlineController;
import org.example.persistence.database.DatabaseConnection;
import org.example.persistence.models.Customer;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Customer Validation Integration Tests")
public class CustomerValidationIntegrationTest {

    private CustomerController customerController;
    private OnlineController onlineController;
    private DatabaseConnection databaseConnection;

    @BeforeEach
    @DisplayName("Setup test environment")
    void setUp() throws Exception {
        customerController = new CustomerController();
        onlineController = new OnlineController();
        databaseConnection = DatabaseConnection.getInstance();
        databaseConnection.connect();
    }

    @Test
    @Order(1)
    @DisplayName("Valid customer should be added successfully")
    void addCustomer_ValidData_Success() throws Exception {
        // Arrange
        Customer validCustomer = new Customer("John Doe Integration", "0771234500");

        // Act & Assert
        assertDoesNotThrow(() -> {
            customerController.add_Customer(validCustomer);
        });

        // Verify customer was added
        Customer retrievedCustomer = customerController.get_Customer_from_contactNumber("0771234500");
        assertNotNull(retrievedCustomer);
        assertEquals("John Doe Integration", retrievedCustomer.getName());
        assertEquals("0771234500", retrievedCustomer.getcontactNumber());
    }

    @Test
    @Order(2)
    @DisplayName("Customer with invalid name should be rejected")
    void addCustomer_InvalidName_ThrowsException() throws Exception {
        // Arrange
        Customer invalidCustomer = new Customer("", "0771234568");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            customerController.add_Customer(invalidCustomer);
        });

        assertTrue(exception.getMessage().contains("Name is required"));
    }

    @Test
    @Order(3)
    @DisplayName("Customer with invalid phone should be rejected")
    void addCustomer_InvalidPhone_ThrowsException() throws Exception {
        // Arrange
        Customer invalidCustomer = new Customer("Jane Doe", "abc123");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            customerController.add_Customer(invalidCustomer);
        });

        assertTrue(exception.getMessage().contains("Phone number is required"));
    }

    @Test
    @Order(4)
    @DisplayName("Valid online customer should be registered successfully")
    void registerOnlineCustomer_ValidData_Success() throws Exception {
        // Arrange
        Customer validCustomer = new Customer("Alice Smith", "0771234569");
        String validEmail = "alice@example.com";
        String validAddress = "123 Main Street, City";

        // Act & Assert
        assertDoesNotThrow(() -> {
            boolean result = onlineController.registerOnlineCustomer(validCustomer, validEmail, validAddress);
            assertTrue(result);
        });

        // Verify customer was registered with an ID
        assertTrue(validCustomer.getId() > 0);
    }

    @Test
    @Order(5)
    @DisplayName("Online customer with invalid email should be rejected")
    void registerOnlineCustomer_InvalidEmail_ThrowsException() throws Exception {
        // Arrange
        Customer customer = new Customer("Bob Johnson", "0771234570");
        String invalidEmail = "invalid-email-format";
        String validAddress = "456 Oak Avenue, City";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            onlineController.registerOnlineCustomer(customer, invalidEmail, validAddress);
        });

        assertTrue(exception.getMessage().contains("Email must be in valid format"));
    }

    @Test
    @Order(6)
    @DisplayName("Online customer without email should be rejected")
    void registerOnlineCustomer_MissingEmail_ThrowsException() throws Exception {
        // Arrange
        Customer customer = new Customer("Charlie Brown", "0771234571");
        String validAddress = "789 Pine Street, City";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            onlineController.registerOnlineCustomer(customer, null, validAddress);
        });

        assertTrue(exception.getMessage().contains("Email is required for online customers"));
    }

    @Test
    @Order(7)
    @DisplayName("Online customer without address should be rejected")
    void registerOnlineCustomer_MissingAddress_ThrowsException() throws Exception {
        // Arrange
        Customer customer = new Customer("Diana Wilson", "0771234572");
        String validEmail = "diana@example.com";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            onlineController.registerOnlineCustomer(customer, validEmail, null);
        });

        assertTrue(exception.getMessage().contains("Address is required for online customers"));
    }

    @Test
    @Order(8)
    @DisplayName("Customer with special characters in name should be accepted")
    void addCustomer_SpecialCharactersInName_Success() throws Exception {
        // Arrange
        Customer customerWithSpecialChars = new Customer("María José O'Connor-Smith", "0771234573");

        // Act & Assert
        assertDoesNotThrow(() -> {
            customerController.add_Customer(customerWithSpecialChars);
        });

        // Verify customer was added with special characters preserved
        Customer retrievedCustomer = customerController.get_Customer_from_contactNumber("0771234573");
        assertNotNull(retrievedCustomer);
        assertEquals("María José O'Connor-Smith", retrievedCustomer.getName());
    }

    @Test
    @Order(9)
    @DisplayName("Customer with international phone number should be accepted")
    void addCustomer_InternationalPhone_Success() throws Exception {
        // Arrange
        Customer internationalCustomer = new Customer("Hans Mueller", "+49-30-12345678");

        // Act & Assert
        assertDoesNotThrow(() -> {
            customerController.add_Customer(internationalCustomer);
        });

        // Verify customer was added
        Customer retrievedCustomer = customerController.get_Customer_from_contactNumber("+49-30-12345678");
        assertNotNull(retrievedCustomer);
        assertEquals("Hans Mueller", retrievedCustomer.getName());
    }

    @Test
    @Order(10)
    @DisplayName("Online customer with valid international email should be accepted")
    void registerOnlineCustomer_InternationalEmail_Success() throws Exception {
        // Arrange
        Customer customer = new Customer("Jean-Pierre Dubois", "0771234574");
        String internationalEmail = "jean-pierre@francais.fr";
        String address = "123 Rue de la Paix, Paris, France";

        // Act & Assert
        assertDoesNotThrow(() -> {
            boolean result = onlineController.registerOnlineCustomer(customer, internationalEmail, address);
            assertTrue(result);
        });

        // Verify customer was registered
        assertTrue(customer.getId() > 0);
    }
}
