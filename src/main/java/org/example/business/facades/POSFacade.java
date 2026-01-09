package org.example.business.facades;

import org.example.core.config.SystemConfig;
import org.example.business.validators.TransactionValidator;

import org.example.shared.dto.BillDTO;
import org.example.shared.dto.BillItemDTO;
import org.example.shared.dto.ItemDTO;
import org.example.persistence.gateways.ItemGateway;
import org.example.business.managers.StockManager;
import org.example.core.state.CheckoutContext;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class POSFacade {
    private static POSFacade instance;
    private static final Object lock = new Object();
    
    private final ItemGateway itemGateway;
    private final StockManager stockManager;
    private CheckoutContext currentCheckout;

    private POSFacade() {
        this.itemGateway = ItemGateway.getInstance();
        this.stockManager = StockManager.getInstance();
    }

    public static POSFacade getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new POSFacade();
                }
            }
        }
        return instance;
    }

    public void startCheckout(String transactionType) {
        // Validate transaction type
        if (!TransactionValidator.isValidTransactionType(transactionType)) {
            throw new IllegalArgumentException("Invalid transaction type: " + transactionType + 
                ". Must be COUNTER or ONLINE");
        }
        
        // POS Facade should only handle COUNTER transactions
        if (!SystemConfig.TRANSACTION_COUNTER.equals(transactionType)) {
            throw new IllegalArgumentException("POS Facade can only handle COUNTER transactions. " +
                "Online transactions should use OnlineStoreFacade");
        }
        
        currentCheckout = new CheckoutContext(transactionType);
        System.out.println("Started new " + transactionType + " checkout session");
    }

    public void addItemToCheckout(String itemCode, int quantity) {
        if (currentCheckout == null) {
            throw new IllegalStateException("No active checkout session. Call startCheckout() first.");
        }

        try {
            ItemDTO item = itemGateway.findByCode(itemCode);
            if (item == null) {
                throw new IllegalArgumentException("Item not found: " + itemCode);
            }

            if (!stockManager.hasEnoughStock(item.getId(), quantity)) {
                throw new IllegalArgumentException("Insufficient stock for item: " + itemCode);
            }

            currentCheckout.addItem(itemCode, quantity);
        } catch (Exception e) {
            System.err.println("Error adding item: " + e.getMessage());
        }
    }

    public void removeItemFromCheckout(String itemCode) {
        if (currentCheckout == null) {
            throw new IllegalStateException("No active checkout session");
        }
        currentCheckout.removeItem(itemCode);
    }
    public void applyDiscount(double discount) {
        if (currentCheckout == null) {
            throw new IllegalStateException("No active checkout session");
        }
        currentCheckout.applyDiscount(discount);
    }

    // Set customer information for current checkout
    public void setCustomer(int customerId, String customerName, String customerPhone) {
        if (currentCheckout == null) {
            throw new IllegalStateException("No active checkout session");
        }
        currentCheckout.setCustomerId(customerId);
        currentCheckout.setCustomerName(customerName);
        currentCheckout.setCustomerPhone(customerPhone);
    }

    // Process payment and complete transaction
    public BillDTO completeTransaction(double cashTendered) {
        if (currentCheckout == null) {
            throw new IllegalStateException("No active checkout session");
        }

        try {
            // Validate cash payment for transaction type
            String transactionType = currentCheckout.getTransactionType();
            if (!TransactionValidator.isCashPaymentValid(transactionType, cashTendered)) {
                throw new IllegalArgumentException("Invalid cash amount for " + transactionType + 
                    " transaction. COUNTER transactions require cash > 0");
            }
            
            currentCheckout.processPayment(cashTendered);
            currentCheckout.generateBill();
            BillDTO bill = currentCheckout.getGeneratedBill();
            
            // Ensure proper store type is set
            String expectedStoreType = TransactionValidator.getStoreTypeForTransaction(transactionType);
            if (!expectedStoreType.equals(bill.getStoreType())) {
                System.out.println("Correcting store type from " + bill.getStoreType() + " to " + expectedStoreType);
            }
            
            updateStockLevels(currentCheckout.getItems());
            currentCheckout.printBill();
            currentCheckout = null;
            
            return bill;
            
        } catch (Exception e) {
            System.err.println("Error completing transaction: " + e.getMessage());
            return null;
        }
    }

    
    // Get current checkout total
    public double getCurrentTotal() {
        if (currentCheckout == null) {
            return 0.0;
        }
        return currentCheckout.calculateTotal();
    }

    // Get current checkout items
    public List<BillItemDTO> getCurrentItems() {
        if (currentCheckout == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(currentCheckout.getItems());
    }

    // Cancel current checkout
    public void cancelCheckout() {
        if (currentCheckout != null) {
            currentCheckout = null;
            System.out.println("Checkout session cancelled");
        }
    }

    // Quick sale method for simple transactions
    public BillDTO quickSale(String itemCode, int quantity, int customerId, 
                            String customerName, String customerPhone, 
                            double discount, double cashTendered) {
        try {
            startCheckout("COUNTER");
            setCustomer(customerId, customerName, customerPhone);
            addItemToCheckout(itemCode, quantity);
            if (discount > 0) {
                applyDiscount(discount);
            }
            return completeTransaction(cashTendered);
        } catch (Exception e) {
            System.err.println("Error in quick sale: " + e.getMessage());
            cancelCheckout();
            return null;
        }
    }

    public ItemDTO getItemInfo(String itemCode) {
        try {
            return itemGateway.findByCode(itemCode);
        } catch (Exception e) {
            System.err.println("Error getting item info: " + e.getMessage());
            return null;
        }
    }

    public int getStockQuantity(String itemCode) {
        try {
            ItemDTO item = itemGateway.findByCode(itemCode);
            if (item == null) {
                return 0;
            }
            return stockManager.getTotalStockQuantity(item.getId());
        } catch (Exception e) {
            System.err.println("Error getting stock quantity: " + e.getMessage());
            return 0;
        }
    }

    private void updateStockLevels(List<BillItemDTO> items) throws SQLException, ClassNotFoundException {
        for (BillItemDTO item : items) {
            stockManager.reduceStock(item.getItemId(), item.getQuantity());
        }
    }
}
