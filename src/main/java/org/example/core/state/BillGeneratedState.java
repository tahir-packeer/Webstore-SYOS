package org.example.core.state;

import org.example.shared.dto.BillDTO;
import org.example.shared.dto.BillItemDTO;

public class BillGeneratedState implements CheckoutState {

    @Override
    public void addItem(CheckoutContext context, String itemCode, int quantity) {
        System.err.println("Cannot add items: Bill already generated");
    }

    @Override
    public void removeItem(CheckoutContext context, String itemCode) {
        System.err.println("Cannot remove items: Bill already generated");
    }

    @Override
    public void applyDiscount(CheckoutContext context, double discount) {
        System.err.println("Cannot apply discount: Bill already generated");
    }

    @Override
    public void processPayment(CheckoutContext context, double cashTendered) {
        System.err.println("Cannot process payment: Bill already generated");
    }

    @Override
    public void generateBill(CheckoutContext context) {
    }

    @Override
    public void printBill(CheckoutContext context) {
        BillDTO bill = context.getGeneratedBill();
        if (bill == null) {
            System.err.println("No bill to print");
            return;
        }

        System.out.println("========== Synex Outlet Store ==========");
        System.out.println("Invoice No: " + bill.getInvoiceNumber());
        System.out.println("Date: " + bill.getBillDate());

        if (bill.getCustomerName() != null) {
            System.out.println("Customer: " + bill.getCustomerName());
            System.out.println("Phone: " + bill.getCustomerPhone());
        } else {
            System.out.println("Customer: Walk-in");
        }

        System.out.println("Transaction Type: " + bill.getTransactionType());
        System.out.println("Store Type: " + bill.getStoreType());
        System.out.println("========================================");

        System.out.printf("%-5s %-20s %-10s %-10s %-15s%n", "#", "Item Name", "Price", "Qty", "Total");
        System.out.println("----------------------------------------");

        int itemNumber = 1;
        for (BillItemDTO item : bill.getBillItems()) {
            System.out.printf("%-5d %-20s Rs.%-10.2f %-10d Rs.%-15.2f%n",
                    itemNumber++,
                    item.getItemName(),
                    item.getItemPrice(),
                    item.getQuantity(),
                    item.getTotalPrice());
        }

        System.out.println("----------------------------------------");
        System.out.printf("Subtotal: Rs.%.2f%n", bill.getFullPrice() + bill.getDiscount());
        System.out.printf("Discount: Rs.%.2f%n", bill.getDiscount());
        System.out.printf("Total: Rs.%.2f%n", bill.getFullPrice());

        if (bill.getTransactionType().equals("COUNTER")) {
            System.out.printf("Cash Tendered: Rs.%.2f%n", bill.getCashTendered());
            System.out.printf("Change: Rs.%.2f%n", bill.getChangeAmount());
        }

        System.out.println("========================================");
        System.out.println("Thank you for shopping with us!");
    }

    @Override
    public String getStateName() {
        return "Bill Generated";
    }
}
