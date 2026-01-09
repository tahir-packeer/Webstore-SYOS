package org.example.presentation.controllers;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReportControllerTest {

    private ReportController reportController;

    @BeforeEach
    void setUp() {
        reportController = new ReportController();
    }

    @Test
    @Order(1)
    void generateSalesReport_ValidDate_GeneratesReport() {
        String validDate = "2023-09-08";
        
        assertDoesNotThrow(() -> {
            String report = reportController.generate_sales_report(validDate);
            assertNotNull(report);
        });
    }

    @Test
    @Order(2)
    void generateSalesReport_InvalidDateFormat_HandlesGracefully() {
        String invalidDate = "invalid-date";
        
        assertDoesNotThrow(() -> {
            String report = reportController.generate_sales_report(invalidDate);
            assertNotNull(report);
        });
    }

    @Test
    @Order(3)
    void generateSalesReport_NullDate_HandlesGracefully() {
        assertDoesNotThrow(() -> {
            String report = reportController.generate_sales_report(null);
            assertNotNull(report);
        });
    }

    @Test
    @Order(4)
    void generateSalesReport_EmptyDate_HandlesGracefully() {
        assertDoesNotThrow(() -> {
            String report = reportController.generate_sales_report("");
            assertNotNull(report);
        });
    }

    @Test
    @Order(5)
    void generateSalesReport_FutureDate_HandlesGracefully() {
        String futureDate = "2050-12-31";
        
        assertDoesNotThrow(() -> {
            String report = reportController.generate_sales_report(futureDate);
            assertNotNull(report);
        });
    }

    @Test
    @Order(6)
    void generateSalesReport_WithTransactionType_GeneratesReport() {
        String validDate = "2023-09-08";
        String transactionType = "COUNTER";
        
        assertDoesNotThrow(() -> {
            String report = reportController.generate_sales_report(validDate, transactionType, null);
            assertNotNull(report);
        });
    }

    @Test
    @Order(7)
    void generateSalesReport_WithStoreType_GeneratesReport() {
        String validDate = "2023-09-08";
        String storeType = "STORE";
        
        assertDoesNotThrow(() -> {
            String report = reportController.generate_sales_report(validDate, null, storeType);
            assertNotNull(report);
        });
    }

    @Test
    @Order(8)
    void generateSalesReport_WithBothFilters_GeneratesReport() {
        String validDate = "2023-09-08";
        String transactionType = "COUNTER";
        String storeType = "STORE";
        
        assertDoesNotThrow(() -> {
            String report = reportController.generate_sales_report(validDate, transactionType, storeType);
            assertNotNull(report);
        });
    }

    @Test
    @Order(9)
    void generateSalesReport_InvalidTransactionType_HandlesGracefully() {
        String validDate = "2023-09-08";
        String invalidTransactionType = "INVALID_TYPE";
        
        assertDoesNotThrow(() -> {
            String report = reportController.generate_sales_report(validDate, invalidTransactionType, null);
            assertNotNull(report);
        });
    }

    @Test
    @Order(10)
    void generateSalesReport_InvalidStoreType_HandlesGracefully() {
        String validDate = "2023-09-08";
        String invalidStoreType = "INVALID_STORE";
        
        assertDoesNotThrow(() -> {
            String report = reportController.generate_sales_report(validDate, null, invalidStoreType);
            assertNotNull(report);
        });
    }

    @Test
    @Order(11)
    void generateSalesReport_EmptyTransactionType_HandlesGracefully() {
        String validDate = "2023-09-08";
        String emptyTransactionType = "";
        
        assertDoesNotThrow(() -> {
            String report = reportController.generate_sales_report(validDate, emptyTransactionType, null);
            assertNotNull(report);
        });
    }

    @Test
    @Order(12)
    void generateSalesReport_EmptyStoreType_HandlesGracefully() {
        String validDate = "2023-09-08";
        String emptyStoreType = "";
        
        assertDoesNotThrow(() -> {
            String report = reportController.generate_sales_report(validDate, null, emptyStoreType);
            assertNotNull(report);
        });
    }
}
