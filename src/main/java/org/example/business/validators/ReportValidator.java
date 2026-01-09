package org.example.business.validators;

import org.example.core.config.SystemConfig;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ReportValidator {
    
    // Validate report transaction type filter
    public static boolean isValidReportTransactionType(String transactionType) {
        return transactionType == null || // null means all transaction types
               transactionType.trim().isEmpty() || // empty means all transaction types
               SystemConfig.TRANSACTION_COUNTER.equals(transactionType) ||
               SystemConfig.TRANSACTION_ONLINE.equals(transactionType);
    }
    
    // Validate report store type filter
    public static boolean isValidReportStoreType(String storeType) {
        return storeType == null || // null means all store types
               storeType.trim().isEmpty() || // empty means all store types
               SystemConfig.STORE_TYPE_STORE.equals(storeType) ||
               SystemConfig.STORE_TYPE_WEBSITE.equals(storeType);
    }
    
    // Normalize and validate report filters, returning normalized values
    public static String[] validateAndNormalizeReportFilters(String transactionType, String storeType) {
        // Normalize empty strings to null for consistency
        if (transactionType != null && transactionType.trim().isEmpty()) {
            transactionType = null;
        }
        if (storeType != null && storeType.trim().isEmpty()) {
            storeType = null;
        }
        
        // For invalid inputs, log warning but continue (graceful degradation)
        if (!isValidReportTransactionType(transactionType)) {
            System.out.println("Warning: Invalid transaction type '" + transactionType + "', defaulting to all transactions");
            transactionType = null;
        }
        
        if (!isValidReportStoreType(storeType)) {
            System.out.println("Warning: Invalid store type '" + storeType + "', defaulting to all store types");
            storeType = null;
        }
        
        // If both are specified, ensure they are compatible
        if (transactionType != null && storeType != null) {
            if (!TransactionValidator.isValidStoreType(storeType, transactionType)) {
                System.out.println("Warning: Incompatible transaction type '" + transactionType + 
                    "' and store type '" + storeType + "' combination, defaulting to combined report");
                transactionType = null;
                storeType = null;
            }
        }
        
        return new String[]{transactionType, storeType};
    }

    // Legacy method for backward compatibility
    public static void validateReportFilters(String transactionType, String storeType) {
        // Use the new method but ignore the return value for backward compatibility
        validateAndNormalizeReportFilters(transactionType, storeType);
    }
    
    // Get description for report filters
    public static String getReportDescription(String transactionType, String storeType) {
        StringBuilder desc = new StringBuilder();
        
        if (transactionType == null && storeType == null) {
            desc.append("All Transactions (Combined Report)");
        } else if (transactionType != null && storeType == null) {
            desc.append(transactionType).append(" Transactions");
        } else if (transactionType == null && storeType != null) {
            desc.append(storeType).append(" Store Transactions");
        } else {
            desc.append(transactionType).append(" Transactions from ").append(storeType).append(" Store");
        }
        
        return desc.toString();
    }
    
    public static String validateAndNormalizeDate(String date) {
        if (date == null) {
            System.out.println("Warning: Date is null, using today's date");
            return LocalDate.now().toString();
        }
        
        String trimmedDate = date.trim();
        if (trimmedDate.isEmpty()) {
            System.out.println("Warning: Date is empty, using today's date");
            return LocalDate.now().toString();
        }
        
        try {
            // Try to parse the date to validate it
            LocalDate.parse(trimmedDate, DateTimeFormatter.ISO_LOCAL_DATE);
            return trimmedDate;
        } catch (DateTimeParseException e) {
            System.out.println("Warning: Invalid date format '" + date + "', using today's date");
            return LocalDate.now().toString();
        }
    }
}
