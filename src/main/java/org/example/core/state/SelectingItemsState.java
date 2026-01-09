package org.example.core.state;

public class SelectingItemsState implements CheckoutState {

    @Override
    public void addItem(CheckoutContext context, String itemCode, int quantity) {
        try {
            context.addItemToList(itemCode, quantity);
        } catch (Exception e) {
            System.err.println("Error adding item: " + e.getMessage());
        }
    }

    @Override
    public void removeItem(CheckoutContext context, String itemCode) {
        context.removeItemFromList(itemCode);
    }

    @Override
    public void applyDiscount(CheckoutContext context, double discount) {
        context.setDiscount(discount);
    }

    @Override
    public void processPayment(CheckoutContext context, double cashTendered) {
        if (!context.hasItems()) {
            System.err.println("Cannot process payment: No items in cart");
            return;
        }

        double total = context.calculateTotal();
        if (cashTendered < total && context.getTransactionType().equals("COUNTER")) {
            System.err.println("Insufficient payment. Required: Rs." + total + ", Received: Rs." + cashTendered);
            return;
        }

        context.setCashTendered(cashTendered);
        context.setChangeAmount(cashTendered - total);

        context.setState(new PaymentPendingState());
    }

    @Override
    public void generateBill(CheckoutContext context) {
        System.err.println("Cannot generate bill: Payment not processed yet");
    }

    @Override
    public void printBill(CheckoutContext context) {
        System.err.println("Cannot print bill: Bill not generated yet");
    }

    @Override
    public String getStateName() {
        return "Selecting Items";
    }
}
