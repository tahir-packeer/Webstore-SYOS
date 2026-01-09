package org.example.persistence.models;

import java.util.Date;

public class Stock {
    private int id;
    private Item item;
    private int quantity;
    private Date date_of_expiry;
    private Date date_of_purchase;
    private boolean availability;

    public Stock(Item item, int quantity, Date date_of_expiry) {
        this.item = item;
        this.quantity = quantity;
        this.date_of_expiry = date_of_expiry;
        this.date_of_purchase = new Date();
        this.availability = true;
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
    }
    public int getQuantity() {
        return quantity;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    public Date getDate_of_expiry() {
        return date_of_expiry;
    }
    public void setDate_of_expiry(Date date_of_expiry) {
        this.date_of_expiry = date_of_expiry;
    }
    public Date getDate_of_purchase() {
        return date_of_purchase;
    }
    public void setDate_of_purchase(Date date_of_purchase) {
        this.date_of_purchase = date_of_purchase;
    }
    public boolean isAvailability() {
        return availability;
    }
    public void setAvailability(boolean availability) {
        this.availability = availability;
    }

}
