package org.example.business.validators;

import org.example.core.config.SystemConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Transaction Validator Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TransactionValidatorTest {

    @Test
    @Order(1)
    @DisplayName("Valid transaction types should return true")
    void isValidTransactionType_ValidTypes_ReturnsTrue() {
        assertTrue(TransactionValidator.isValidTransactionType(SystemConfig.TRANSACTION_COUNTER));
        assertTrue(TransactionValidator.isValidTransactionType(SystemConfig.TRANSACTION_ONLINE));
    }

    @Test
    @Order(2)
    @DisplayName("Invalid transaction types should return false")
    void isValidTransactionType_InvalidTypes_ReturnsFalse() {
        assertFalse(TransactionValidator.isValidTransactionType(null));
        assertFalse(TransactionValidator.isValidTransactionType(""));
        assertFalse(TransactionValidator.isValidTransactionType("INVALID"));
        assertFalse(TransactionValidator.isValidTransactionType("CASH"));
        assertFalse(TransactionValidator.isValidTransactionType("CREDIT"));
    }

    @Test
    @Order(3)
    @DisplayName("Valid store type for COUNTER transaction")
    void isValidStoreType_CounterTransaction_ValidatesCorrectly() {
        assertTrue(TransactionValidator.isValidStoreType(
            SystemConfig.STORE_TYPE_STORE, SystemConfig.TRANSACTION_COUNTER));
        
        assertFalse(TransactionValidator.isValidStoreType(
            SystemConfig.STORE_TYPE_WEBSITE, SystemConfig.TRANSACTION_COUNTER));
    }

    @Test
    @Order(4)
    @DisplayName("Valid store type for ONLINE transaction")
    void isValidStoreType_OnlineTransaction_ValidatesCorrectly() {
        assertTrue(TransactionValidator.isValidStoreType(
            SystemConfig.STORE_TYPE_WEBSITE, SystemConfig.TRANSACTION_ONLINE));
        
        assertFalse(TransactionValidator.isValidStoreType(
            SystemConfig.STORE_TYPE_STORE, SystemConfig.TRANSACTION_ONLINE));
    }

    @Test
    @Order(5)
    @DisplayName("Invalid store type for any transaction")
    void isValidStoreType_InvalidStoreType_ReturnsFalse() {
        assertFalse(TransactionValidator.isValidStoreType(null, SystemConfig.TRANSACTION_COUNTER));
        assertFalse(TransactionValidator.isValidStoreType("INVALID", SystemConfig.TRANSACTION_COUNTER));
        assertFalse(TransactionValidator.isValidStoreType("WAREHOUSE", SystemConfig.TRANSACTION_ONLINE));
    }

    @Test
    @Order(6)
    @DisplayName("Invalid transaction type returns false")
    void isValidStoreType_InvalidTransactionType_ReturnsFalse() {
        assertFalse(TransactionValidator.isValidStoreType(
            SystemConfig.STORE_TYPE_STORE, "INVALID_TRANSACTION"));
        assertFalse(TransactionValidator.isValidStoreType(
            SystemConfig.STORE_TYPE_WEBSITE, null));
    }
}
