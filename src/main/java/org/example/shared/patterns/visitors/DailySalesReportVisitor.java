package org.example.shared.patterns.visitors;

import org.example.shared.dto.BillDTO;
import org.example.shared.dto.ItemDTO;
import org.example.shared.dto.StockDTO;

import java.util.List;
import java.time.LocalDate;

public class DailySalesReportVisitor implements ReportVisitor {
    private StringBuilder report;
    private LocalDate reportDate;
    
    public DailySalesReportVisitor(LocalDate reportDate) {
        this.report = new StringBuilder();
        this.reportDate = reportDate;
    }

    @Override
    public void visitBills(List<BillDTO> bills) {
        report.append("===============================\n");
        report.append("      DAILY SALES REPORT       \n");
        report.append("===============================\n");
        report.append("Date: ").append(reportDate).append("\n\n");

        // Filter bills for the specific date
        List<BillDTO> dailyBills = bills.stream()
                .filter(bill -> bill.getBillDate().equals(reportDate))
                .toList();

        if (dailyBills.isEmpty()) {
            report.append("No sales found for this date.\n");
            return;
        }

        double totalSales = 0;
        double totalDiscount = 0;
        int totalTransactions = dailyBills.size();

        report.append(String.format("%-15s %-10s %-12s %-10s %-15s\n",
                "Invoice", "Customer", "Total", "Discount", "Type"));
        report.append("---------------------------------------------------------------\n");

        for (BillDTO bill : dailyBills) {
            String customerName = bill.getCustomerName() != null ? 
                    bill.getCustomerName() : "Walk-in";
            
            report.append(String.format("%-15s %-10s %-12.2f %-10.2f %-15s\n",
                    bill.getInvoiceNumber(),
                    customerName.length() > 10 ? customerName.substring(0, 10) : customerName,
                    bill.getFullPrice(),
                    bill.getDiscount(),
                    bill.getTransactionType()));

            totalSales += bill.getFullPrice();
            totalDiscount += bill.getDiscount();
        }

        report.append("---------------------------------------------------------------\n");
        report.append(String.format("Total Transactions: %d\n", totalTransactions));
        report.append(String.format("Total Sales: Rs.%.2f\n", totalSales));
        report.append(String.format("Total Discounts: Rs.%.2f\n", totalDiscount));
        report.append(String.format("Average Sale: Rs.%.2f\n", totalSales / totalTransactions));
        report.append("===============================\n");
    }

    @Override
    public void visitItems(List<ItemDTO> items) {
    }

    @Override
    public void visitStocks(List<StockDTO> stocks) {
    }

    @Override
    public String getReport() {
        return report.toString();
    }
}
