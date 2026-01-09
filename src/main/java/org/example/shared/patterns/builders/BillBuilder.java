package org.example.shared.patterns.builders;

import org.example.core.config.SystemConfig;
import org.example.business.validators.TransactionValidator;

import org.example.shared.dto.BillDTO;
import org.example.shared.dto.BillItemDTO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BillBuilder {
    private BillDTO bill;
    private List<BillItemDTO> billItems;

    public BillBuilder() {
        this.bill = new BillDTO();
        this.billItems = new ArrayList<>();
        this.bill.setBillDate(LocalDate.now());
    }

    public BillBuilder setCustomer(int customerId, String customerName, String customerPhone) {
        bill.setCustomerId(customerId);
        bill.setCustomerName(customerName);
        bill.setCustomerPhone(customerPhone);
        return this;
    }

    public BillBuilder setInvoiceNumber(String invoiceNumber) {
        bill.setInvoiceNumber(invoiceNumber);
        return this;
    }

    public BillBuilder addItem(int itemId, String itemCode, String itemName, 
                              double itemPrice, int quantity) {
        double totalPrice = itemPrice * quantity;
        BillItemDTO billItem = new BillItemDTO();
        billItem.setItemId(itemId);
        billItem.setItemCode(itemCode);
        billItem.setItemName(itemName);
        billItem.setItemPrice(itemPrice);
        billItem.setQuantity(quantity);
        billItem.setTotalPrice(totalPrice);
        
        billItems.add(billItem);
        return this;
    }

    public BillBuilder addItem(BillItemDTO billItem) {
        billItems.add(billItem);
        return this;
    }

    public BillBuilder setDiscount(double discount) {
        bill.setDiscount(discount);
        return this;
    }

    public BillBuilder setPayment(double cashTendered, double changeAmount) {
        bill.setCashTendered(cashTendered);
        bill.setChangeAmount(changeAmount);
        return this;
    }

    public BillBuilder setTransactionType(String transactionType) {
        // Validate transaction type
        if (!TransactionValidator.isValidTransactionType(transactionType)) {
            throw new IllegalArgumentException("Invalid transaction type: " + transactionType);
        }
        bill.setTransactionType(transactionType);
        return this;
    }

    public BillBuilder setStoreType(String storeType) {
        // Auto-correct store type based on transaction type if bill has transaction type
        if (bill.getTransactionType() != null) {
            String expectedStoreType = TransactionValidator.getStoreTypeForTransaction(bill.getTransactionType());
            if (!expectedStoreType.equals(storeType)) {
                System.out.println("Auto-correcting store type from " + storeType + " to " + expectedStoreType);
                storeType = expectedStoreType;
            }
        }
        bill.setStoreType(storeType);
        return this;
    }

    public BillBuilder setBillDate(LocalDate billDate) {
        bill.setBillDate(billDate);
        return this;
    }

    public BillBuilder calculateTotals() {
        double subtotal = billItems.stream()
                                  .mapToDouble(BillItemDTO::getTotalPrice)
                                  .sum();
        
        double discount = bill.getDiscount();
        double fullPrice = subtotal - discount;
        
        bill.setFullPrice(fullPrice);
        return this;
    }

    public BillDTO build() {
        // Validate required fields
        if (bill.getInvoiceNumber() == null || bill.getInvoiceNumber().isEmpty()) {
            throw new IllegalStateException("Invoice number is required");
        }
        
        // Validate transaction type and store type consistency
        if (bill.getTransactionType() != null && bill.getStoreType() != null) {
            if (!TransactionValidator.isValidStoreType(bill.getStoreType(), bill.getTransactionType())) {
                throw new IllegalStateException("Invalid store type '" + bill.getStoreType() + 
                    "' for transaction type '" + bill.getTransactionType() + "'");
            }
        }
        
        // Validate cash payment rules
        if (bill.getTransactionType() != null) {
            if (!TransactionValidator.isCashPaymentValid(bill.getTransactionType(), bill.getCashTendered())) {
                if (SystemConfig.TRANSACTION_COUNTER.equals(bill.getTransactionType())) {
                    throw new IllegalStateException("COUNTER transactions must have cash tendered > 0");
                } else if (SystemConfig.TRANSACTION_ONLINE.equals(bill.getTransactionType())) {
                    throw new IllegalStateException("ONLINE transactions should have cash tendered = 0 (cash on delivery)");
                }
            }
        }
        
        if (billItems.isEmpty()) {
            throw new IllegalStateException("Bill must contain at least one item");
        }

        // Calculate totals if not already done
        if (bill.getFullPrice() == 0) {
            calculateTotals();
        }

        // Set bill items
        bill.setBillItems(new ArrayList<>(billItems));

        // Set bill ID for each bill item (will be set after saving bill)
        for (BillItemDTO item : billItems) {
            item.setBillId(bill.getId());
        }

        return bill;
    }

    public static BillBuilder newBuilder() {
        return new BillBuilder();
    }

    // Convenience method for quick in-store bill creation
    public static BillBuilder forInStoreSale() {
        return new BillBuilder()
                .setTransactionType("COUNTER")
                .setStoreType("STORE");
    }

    // Convenience method for quick online bill creation
    public static BillBuilder forOnlineSale() {
        return new BillBuilder()
                .setTransactionType("ONLINE")
                .setStoreType("WEBSITE")
                .setPayment(0, 0); // No cash transactions for online
    }
}
