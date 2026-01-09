package org.example.core.state;

import org.example.shared.patterns.builders.BillBuilder;
import org.example.shared.dto.BillDTO;
import org.example.shared.dto.BillItemDTO;
import org.example.persistence.gateways.BillGateway;

public class PaymentPendingState implements CheckoutState {

    @Override
    public void addItem(CheckoutContext context, String itemCode, int quantity) {
        System.err.println("Cannot add items: Payment already processed");
    }

    @Override
    public void removeItem(CheckoutContext context, String itemCode) {
        System.err.println("Cannot remove items: Payment already processed");
    }

    @Override
    public void applyDiscount(CheckoutContext context, double discount) {
        System.err.println("Cannot apply discount: Payment already processed");
    }

    @Override
    public void processPayment(CheckoutContext context, double cashTendered) {
        System.err.println("Payment already processed");
    }

    @Override
    public void generateBill(CheckoutContext context) {
        try {
            BillGateway billGateway = BillGateway.getInstance();
            String invoiceNumber = billGateway.generateInvoiceNumber();

            BillBuilder builder = context.getTransactionType().equals("COUNTER")
                    ? BillBuilder.forInStoreSale()
                    : BillBuilder.forOnlineSale();

            BillDTO bill = builder
                    .setCustomer(context.getCustomerId(), context.getCustomerName(), context.getCustomerPhone())
                    .setInvoiceNumber(invoiceNumber)
                    .setDiscount(context.getDiscount())
                    .setPayment(context.getCashTendered(), context.getChangeAmount())
                    .build();

            // Add items to bill
            for (BillItemDTO item : context.getItems()) {
                bill.getBillItems().add(item);
            }

            // Save to database
            billGateway.insert(bill);
            billGateway.insertBillItems(bill.getBillItems());

            context.setGeneratedBill(bill);
            System.out.println("Bill generated successfully. Invoice: " + invoiceNumber);
            context.setState(new BillGeneratedState());

        } catch (Exception e) {
            System.err.println("Error generating bill: " + e.getMessage());
        }
    }

    @Override
    public void printBill(CheckoutContext context) {
        System.err.println("Cannot print bill: Bill not generated yet");
    }

    @Override
    public String getStateName() {
        return "Payment Pending";
    }
}
