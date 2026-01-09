package org.example.persistence.models;

public class Shelf {
    private int Id;
    private Item item;
    private int quantity;
    private String type; // "STORE" or "WEBSITE" to distinguish physical shelf from website inventory

    public Shelf(Item item, int quantity, String type) {
        this.item = item;
        this.quantity = quantity;
        this.type = type;
    }

    public int getId() {
        return Id;
    }
    public void setId(int id) {
        Id = id;
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
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

}
