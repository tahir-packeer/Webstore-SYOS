package org.example.persistence.models;

public class Shelf_Stock_Information {

    private int shelfId;
    private String itemCode;
    private String itemName;
    private int shelfQuantity;
    private String type;
    private int totalStockQuantity;

    public Shelf_Stock_Information(int shelfId, String itemCode, String itemName, int shelfQuantity, String type, int totalStockQuantity) {
        this.shelfId = shelfId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.shelfQuantity = shelfQuantity;
        this.type = type;
        this.totalStockQuantity = totalStockQuantity;
    }

    public int getShelfId() {
        return shelfId;
    }
    public void setShelfId(int shelfId) {
        this.shelfId = shelfId;
    }
    public String getItemCode() {
        return itemCode;
    }
    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }
    public String getItemName() {
        return itemName;
    }
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    public int getShelfQuantity() {
        return shelfQuantity;
    }
    public void setShelfQuantity(int shelfQuantity) {
        this.shelfQuantity = shelfQuantity;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public int getTotalStockQuantity() {
        return totalStockQuantity;
    }
    public void setTotalStockQuantity(int totalStockQuantity) {
        this.totalStockQuantity = totalStockQuantity;
    }

}
