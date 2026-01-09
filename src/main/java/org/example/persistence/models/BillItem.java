package org.example.persistence.models;

public class BillItem {
    private int id;
    private Item item;
    private Bill bill;
    private int quantity;
    private double itemPrice;
    private double totalPrice;

    public BillItem(Item item, int quantity) {
        this.item = item;
        this.quantity = quantity;
        this.itemPrice = item.getPrice();
        this.totalPrice = itemPrice * quantity;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public Item getItem() {
        return item;
    }
    public void setItem(Item item) {
        this.item = item;
        this.itemPrice = item.getPrice();
        this.totalPrice = itemPrice * quantity;
    }

    public Bill getBill() {
        return bill;
    }
    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public int getQuantity() {
        return quantity;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.totalPrice = itemPrice * quantity;
    }
    public double getItemPrice() {
        return itemPrice;
    }
    public void setItemPrice(double itemPrice) {
        this.itemPrice = itemPrice;
        this.totalPrice = itemPrice * quantity;
    }
    public double getTotalPrice() {
        return totalPrice;
    }
    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

}
