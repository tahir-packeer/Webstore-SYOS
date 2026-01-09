package org.example.business.validators;

import org.example.core.config.SystemConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Report Validator Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReportValidatorTest {

    @Test
    @Order(1)
    @DisplayName("Valid transaction types should return true")
    void isValidReportTransactionType_ValidTypes_ReturnsTrue() {
        assertTrue(ReportValidator.isValidReportTransactionType(null));
        assertTrue(ReportValidator.isValidReportTransactionType(""));
        assertTrue(ReportValidator.isValidReportTransactionType("   "));
        assertTrue(ReportValidator.isValidReportTransactionType(SystemConfig.TRANSACTION_COUNTER));
        assertTrue(ReportValidator.isValidReportTransactionType(SystemConfig.TRANSACTION_ONLINE));
    }

    @Test
    @Order(2)
    @DisplayName("Invalid transaction types should return false")
    void isValidReportTransactionType_InvalidTypes_ReturnsFalse() {
        assertFalse(ReportValidator.isValidReportTransactionType("INVALID"));
        assertFalse(ReportValidator.isValidReportTransactionType("CASH"));
        assertFalse(ReportValidator.isValidReportTransactionType("CREDIT"));
        assertFalse(ReportValidator.isValidReportTransactionType("DEBIT"));
    }

    @Test
    @Order(3)
    @DisplayName("Valid store types should return true")
    void isValidReportStoreType_ValidTypes_ReturnsTrue() {
        assertTrue(ReportValidator.isValidReportStoreType(null));
        assertTrue(ReportValidator.isValidReportStoreType(""));
        assertTrue(ReportValidator.isValidReportStoreType("   "));
        assertTrue(ReportValidator.isValidReportStoreType(SystemConfig.STORE_TYPE_STORE));
        assertTrue(ReportValidator.isValidReportStoreType(SystemConfig.STORE_TYPE_WEBSITE));
    }

    @Test
    @Order(4)
    @DisplayName("Invalid store types should return false")
    void isValidReportStoreType_InvalidTypes_ReturnsFalse() {
        assertFalse(ReportValidator.isValidReportStoreType("INVALID"));
        assertFalse(ReportValidator.isValidReportStoreType("WAREHOUSE"));
        assertFalse(ReportValidator.isValidReportStoreType("MOBILE"));
        assertFalse(ReportValidator.isValidReportStoreType("KIOSK"));
    }
}
