package org.example.persistence.models;

import java.time.LocalDate;

public class Bill {
    private int id;
    private Customer customer;
    private String invoiceNumber;
    private double fullPrice;
    private double discount;
    private double cashTendered;
    private double changeAmount;
    private LocalDate billDate;
    private String transactionType;
    private String storeType;

    public Bill(Customer customer, String invoiceNumber, double fullPrice, double discount, double cashTendered, double changeAmount) {
        this.customer = customer;
        this.invoiceNumber = invoiceNumber;
        this.fullPrice = fullPrice;
        this.discount = discount;
        this.cashTendered = cashTendered;
        this.changeAmount = changeAmount;
        this.billDate = LocalDate.now();
        this.transactionType = "COUNTER"; 
        this.storeType = "STORE"; 
    }

    public Bill(Customer customer, String invoiceNumber, double fullPrice, double discount, double cashTendered, double changeAmount, String transactionType, String storeType) {
        this.customer = customer;
        this.invoiceNumber = invoiceNumber;
        this.fullPrice = fullPrice;
        this.discount = discount;
        this.cashTendered = cashTendered;
        this.changeAmount = changeAmount;
        this.billDate = LocalDate.now();
        this.transactionType = transactionType;
        this.storeType = storeType;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public Customer getCustomer() {
        return customer;
    }
    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
    public String getInvoiceNumber() {
        return invoiceNumber;
    }
    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }
    public double getFullPrice() {
        return fullPrice;
    }
    public void setFullPrice(double fullPrice) {
        this.fullPrice = fullPrice;
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

    public LocalDate getBillDate() {
        return billDate;
    }
    public void setBillDate(LocalDate billDate) {
        this.billDate = billDate;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getStoreType() {
        return storeType;
    }

    public void setStoreType(String storeType) {
        this.storeType = storeType;
    }

}
