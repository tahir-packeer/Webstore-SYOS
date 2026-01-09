package org.example.persistence.models;

import java.util.Date;

/**
 * Enhanced Stock Batch model for SYOS inventory management
 * Supports FIFO operations, expiry tracking, and batch-based inventory
 */
public class StockBatch {
    private int id;
    private Item item;
    private int originalQuantity;
    private int currentQuantity;
    private Date purchaseDate;
    private Date expiryDate;
    private double purchasePrice;
    private String supplierInfo;
    private boolean isAvailable;
    private int daysUntilExpiry;
    
    // Constructors
    public StockBatch() {
        this.purchaseDate = new Date();
        this.isAvailable = true;
    }
    
    public StockBatch(Item item, int quantity, Date expiryDate) {
        this();
        this.item = item;
        this.originalQuantity = quantity;
        this.currentQuantity = quantity;
        this.expiryDate = expiryDate;
        this.calculateDaysUntilExpiry();
    }
    
    public StockBatch(Item item, int quantity, Date expiryDate, double purchasePrice, String supplierInfo) {
        this(item, quantity, expiryDate);
        this.purchasePrice = purchasePrice;
        this.supplierInfo = supplierInfo;
    }
    
    /**
     * Calculate days until expiry from current date
     */
    public void calculateDaysUntilExpiry() {
        if (expiryDate != null) {
            long diffInMillies = expiryDate.getTime() - new Date().getTime();
            this.daysUntilExpiry = (int) (diffInMillies / (1000 * 60 * 60 * 24));
        }
    }
    
    /**
     * Check if this batch is expired
     */
    public boolean isExpired() {
        calculateDaysUntilExpiry();
        return daysUntilExpiry < 0;
    }
    
    /**
     * Check if this batch is near expiry (within 7 days)
     */
    public boolean isNearExpiry() {
        calculateDaysUntilExpiry();
        return daysUntilExpiry >= 0 && daysUntilExpiry <= 7;
    }
    
    /**
     * Check if this batch has sufficient quantity
     */
    public boolean hasQuantity(int required) {
        return isAvailable && currentQuantity >= required;
    }
    
    /**
     * Reduce quantity when items are sold
     */
    public boolean reduceQuantity(int amount) {
        if (hasQuantity(amount)) {
            this.currentQuantity -= amount;
            if (this.currentQuantity == 0) {
                this.isAvailable = false;
            }
            return true;
        }
        return false;
    }
    
    /**
     * Get stock status for reporting
     */
    public String getStockStatus() {
        if (!isAvailable) return "OUT_OF_STOCK";
        if (isExpired()) return "EXPIRED";
        if (isNearExpiry()) return "NEAR_EXPIRY";
        if (currentQuantity < originalQuantity * 0.2) return "LOW_STOCK";
        return "AVAILABLE";
    }
    
    /**
     * Calculate stock turnover percentage
     */
    public double getStockTurnover() {
        return ((double)(originalQuantity - currentQuantity) / originalQuantity) * 100;
    }
    
    // Getters and Setters
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
    
    public int getOriginalQuantity() {
        return originalQuantity;
    }
    
    public void setOriginalQuantity(int originalQuantity) {
        this.originalQuantity = originalQuantity;
    }
    
    public int getCurrentQuantity() {
        return currentQuantity;
    }
    
    public void setCurrentQuantity(int currentQuantity) {
        this.currentQuantity = currentQuantity;
        if (this.currentQuantity <= 0) {
            this.isAvailable = false;
        }
    }
    
    public Date getPurchaseDate() {
        return purchaseDate;
    }
    
    public void setPurchaseDate(Date purchaseDate) {
        this.purchaseDate = purchaseDate;
    }
    
    public Date getExpiryDate() {
        return expiryDate;
    }
    
    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
        this.calculateDaysUntilExpiry();
    }
    
    public double getPurchasePrice() {
        return purchasePrice;
    }
    
    public void setPurchasePrice(double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }
    
    public String getSupplierInfo() {
        return supplierInfo;
    }
    
    public void setSupplierInfo(String supplierInfo) {
        this.supplierInfo = supplierInfo;
    }
    
    public boolean isAvailable() {
        return isAvailable;
    }
    
    public void setAvailable(boolean available) {
        isAvailable = available;
    }
    
    public int getDaysUntilExpiry() {
        calculateDaysUntilExpiry();
        return daysUntilExpiry;
    }
    
    public void setDaysUntilExpiry(int daysUntilExpiry) {
        this.daysUntilExpiry = daysUntilExpiry;
    }
    
    @Override
    public String toString() {
        return "StockBatch{" +
                "id=" + id +
                ", item=" + (item != null ? item.getName() : "null") +
                ", currentQuantity=" + currentQuantity +
                ", expiryDate=" + expiryDate +
                ", daysUntilExpiry=" + daysUntilExpiry +
                ", status='" + getStockStatus() + '\'' +
                '}';
    }
}
