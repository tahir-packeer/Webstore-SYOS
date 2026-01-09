package org.example.business.validators;

import org.example.core.config.SystemConfig;

public class TransactionValidator {
    
    public static boolean isValidTransactionType(String transactionType) {
        return SystemConfig.TRANSACTION_COUNTER.equals(transactionType) || 
               SystemConfig.TRANSACTION_ONLINE.equals(transactionType);
    }
    
    
    public static boolean isValidStoreType(String storeType, String transactionType) {
        if (SystemConfig.TRANSACTION_COUNTER.equals(transactionType)) {
            return SystemConfig.STORE_TYPE_STORE.equals(storeType);
        }
        
        // ONLINE transactions must be WEBSITE type
        if (SystemConfig.TRANSACTION_ONLINE.equals(transactionType)) {
            return SystemConfig.STORE_TYPE_WEBSITE.equals(storeType);
        }
        
        return false;
    }
    
    // Get correct store type for transaction type
    public static String getStoreTypeForTransaction(String transactionType) {
        if (SystemConfig.TRANSACTION_COUNTER.equals(transactionType)) {
            return SystemConfig.STORE_TYPE_STORE;
        } else if (SystemConfig.TRANSACTION_ONLINE.equals(transactionType)) {
            return SystemConfig.STORE_TYPE_WEBSITE;
        }
        throw new IllegalArgumentException("Invalid transaction type: " + transactionType);
    }
    
    public static boolean isCashPaymentValid(String transactionType, double cashTendered) {
        // COUNTER transactions must have cash tendered > 0
        if (SystemConfig.TRANSACTION_COUNTER.equals(transactionType)) {
            return cashTendered > 0;
        }
        
        // ONLINE transactions should have cash tendered = 0
        if (SystemConfig.TRANSACTION_ONLINE.equals(transactionType)) {
            return cashTendered == 0;
        }
        
        return false;
    }
    
    public static boolean canAccessInventory(String transactionType, String inventoryType) {
        if (SystemConfig.TRANSACTION_COUNTER.equals(transactionType)) {
            return "STORE".equals(inventoryType);
        }
        
        if (SystemConfig.TRANSACTION_ONLINE.equals(transactionType)) {
            return "WEBSITE".equals(inventoryType);
        }
        
        return false;
    }
}
