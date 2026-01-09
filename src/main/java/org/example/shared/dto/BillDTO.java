package org.example.shared.dto;

import java.time.LocalDate;
import java.util.List;

public class BillDTO {
    private int id;
    private int customerId;
    private String customerName;
    private String customerPhone;
    private String invoiceNumber;
    private double fullPrice;
    private double discount;
    private double cashTendered;
    private double changeAmount;
    private LocalDate billDate;
    private String transactionType;
    private String storeType;
    private List<BillItemDTO> billItems;

    public BillDTO() {}

    public BillDTO(int id, int customerId, String customerName, String customerPhone,
                   String invoiceNumber, double fullPrice, double discount, 
                   double cashTendered, double changeAmount, LocalDate billDate,
                   String transactionType, String storeType, List<BillItemDTO> billItems) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.invoiceNumber = invoiceNumber;
        this.fullPrice = fullPrice;
        this.discount = discount;
        this.cashTendered = cashTendered;
        this.changeAmount = changeAmount;
        this.billDate = billDate;
        this.transactionType = transactionType;
        this.storeType = storeType;
        this.billItems = billItems;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public List<BillItemDTO> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItemDTO> billItems) {
        this.billItems = billItems;
    }

    @Override
    public String toString() {
        return "BillDTO{" +
                "id=" + id +
                ", customerId=" + customerId +
                ", customerName='" + customerName + '\'' +
                ", customerPhone='" + customerPhone + '\'' +
                ", invoiceNumber='" + invoiceNumber + '\'' +
                ", fullPrice=" + fullPrice +
                ", discount=" + discount +
                ", cashTendered=" + cashTendered +
                ", changeAmount=" + changeAmount +
                ", billDate=" + billDate +
                ", transactionType='" + transactionType + '\'' +
                ", storeType='" + storeType + '\'' +
                ", billItems=" + billItems +
                '}';
    }
}
