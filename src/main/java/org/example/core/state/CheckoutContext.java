package org.example.core.state;

import org.example.shared.dto.BillDTO;
import org.example.shared.dto.BillItemDTO;
import org.example.persistence.gateways.ItemGateway;
import org.example.shared.dto.ItemDTO;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CheckoutContext {
    private CheckoutState currentState;
    private List<BillItemDTO> items;
    private double discount;
    private double cashTendered;
    private double changeAmount;
    private BillDTO generatedBill;
    private int customerId;
    private String customerName;
    private String customerPhone;
    private String deliveryAddress;
    private String transactionType;
    private ItemGateway itemGateway;

    public CheckoutContext(String transactionType) {
        this.items = new ArrayList<>();
        this.discount = 0.0;
        this.cashTendered = 0.0;
        this.changeAmount = 0.0;
        this.transactionType = transactionType;
        this.itemGateway = ItemGateway.getInstance();

        // Start in selecting items state
        setState(new SelectingItemsState());
    }

    public void setState(CheckoutState state) {
        this.currentState = state;
        System.out.println("Checkout state changed to: " + state.getStateName());
    }

    public void addItem(String itemCode, int quantity) {
        currentState.addItem(this, itemCode, quantity);
    }

    public void removeItem(String itemCode) {
        currentState.removeItem(this, itemCode);
    }

    public void applyDiscount(double discount) {
        currentState.applyDiscount(this, discount);
    }

    public void processPayment(double cashTendered) {
        currentState.processPayment(this, cashTendered);
    }

    public void generateBill() {
        currentState.generateBill(this);
    }

    public void printBill() {
        currentState.printBill(this);
    }

    // Helper method to add item to the list
    public void addItemToList(String itemCode, int quantity) throws SQLException, ClassNotFoundException {
        ItemDTO item = itemGateway.findByCode(itemCode);
        if (item == null) {
            throw new IllegalArgumentException("Item not found: " + itemCode);
        }

        // Check if item already exists in the list
        for (BillItemDTO existingItem : items) {
            if (existingItem.getItemCode().equals(itemCode)) {
                existingItem.setQuantity(existingItem.getQuantity() + quantity);
                existingItem.setTotalPrice(existingItem.getItemPrice() * existingItem.getQuantity());
                return;
            }
        }

        // Add new item
        BillItemDTO billItem = new BillItemDTO();
        billItem.setItemId(item.getId());
        billItem.setItemCode(item.getCode());
        billItem.setItemName(item.getName());
        billItem.setItemPrice(item.getPrice());
        billItem.setQuantity(quantity);
        billItem.setTotalPrice(item.getPrice() * quantity);

        items.add(billItem);
    }

    public void removeItemFromList(String itemCode) {
        items.removeIf(item -> item.getItemCode().equals(itemCode));
    }

    public double calculateSubtotal() {
        return items.stream().mapToDouble(BillItemDTO::getTotalPrice).sum();
    }

    public double calculateTotal() {
        return calculateSubtotal() - discount;
    }

    public boolean hasItems() {
        return !items.isEmpty();
    }

    // Getters and Setters
    public CheckoutState getCurrentState() {
        return currentState;
    }

    public List<BillItemDTO> getItems() {
        return items;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getCashTendered() {
        return cashTendered;
    }

    public void setCashTendered(double cashTendered) {
        this.cashTendered = cashTendered;
    }

    public double getChangeAmount() {
        return changeAmount;
    }

    public void setChangeAmount(double changeAmount) {
        this.changeAmount = changeAmount;
    }

    public BillDTO getGeneratedBill() {
        return generatedBill;
    }

    public void setGeneratedBill(BillDTO generatedBill) {
        this.generatedBill = generatedBill;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getTransactionType() {
        return transactionType;
    }
}
