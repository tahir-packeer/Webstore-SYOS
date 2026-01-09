package org.example.core.config;

public class SystemConfig {

    // Business Rules
    public static final int REORDER_THRESHOLD = 50;
    public static final double FREE_SHIPPING_THRESHOLD = 50.00;
    public static final double SHIPPING_COST = 5.99;

    // System Constants
    public static final String INVOICE_PREFIX = "";

    // Transaction Types
    public static final String TRANSACTION_COUNTER = "COUNTER";
    public static final String TRANSACTION_ONLINE = "ONLINE";

    // Store Types
    public static final String STORE_TYPE_STORE = "STORE";
    public static final String STORE_TYPE_WEBSITE = "WEBSITE";

    // User Roles
    public static final String ROLE_CASHIER = "cashier";
    public static final String ROLE_STORE_MANAGER = "storemanager";
    public static final String ROLE_MANAGER = "manager";

    private SystemConfig() {
        // Utility class
    }
}
