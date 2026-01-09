package org.example.shared.patterns.factories;

import org.example.shared.dto.BillDTO;
import org.example.shared.dto.BillItemDTO;

import java.util.List;

public abstract class BillFactory {
    
    public abstract BillDTO createBill(int customerId, String customerName, String customerPhone,
                                      String invoiceNumber, List<BillItemDTO> billItems, 
                                      double discount, double cashTendered, double changeAmount);
    
    protected double calculateTotal(List<BillItemDTO> billItems) {
        return billItems.stream()
                       .mapToDouble(BillItemDTO::getTotalPrice)
                       .sum();
    }
    
    public static BillFactory getFactory(String type) {
        switch (type.toUpperCase()) {
            case "COUNTER":
            case "STORE":
                return new InStoreBillFactory();
            case "ONLINE":
            case "WEBSITE":
                return new OnlineBillFactory();
            default:
                throw new IllegalArgumentException("Unknown bill type: " + type);
        }
    }
}
