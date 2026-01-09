package org.example.shared.patterns.factories;

import org.example.shared.dto.BillDTO;
import org.example.shared.dto.BillItemDTO;

import java.time.LocalDate;
import java.util.List;

public class OnlineBillFactory extends BillFactory {
    
    @Override
    public BillDTO createBill(int customerId, String customerName, String customerPhone,
                             String invoiceNumber, List<BillItemDTO> billItems, 
                             double discount, double cashTendered, double changeAmount) {
        
        double fullPrice = calculateTotal(billItems) - discount;
        
        BillDTO bill = new BillDTO();
        bill.setCustomerId(customerId);
        bill.setCustomerName(customerName);
        bill.setCustomerPhone(customerPhone);
        bill.setInvoiceNumber(invoiceNumber);
        bill.setFullPrice(fullPrice);
        bill.setDiscount(discount);
        bill.setCashTendered(0); // No cash for online transactions
        bill.setChangeAmount(0); // No change for online transactions
        bill.setBillDate(LocalDate.now());
        bill.setTransactionType("ONLINE");
        bill.setStoreType("WEBSITE");
        bill.setBillItems(billItems);
        
        // Set bill ID for each bill item
        for (BillItemDTO item : billItems) {
            item.setBillId(bill.getId());
        }
        
        return bill;
    }
}
