package org.example.core.state;

// State interface for checkout flow - Models checkout flow: Selecting Items → Payment Pending → Bill Generated
// Prevents invalid operations (e.g., can't print bill before payment)
public interface CheckoutState {
    void addItem(CheckoutContext context, String itemCode, int quantity);

    void removeItem(CheckoutContext context, String itemCode);

    void applyDiscount(CheckoutContext context, double discount);

    void processPayment(CheckoutContext context, double cashTendered);

    void generateBill(CheckoutContext context);

    void printBill(CheckoutContext context);

    String getStateName();
}
