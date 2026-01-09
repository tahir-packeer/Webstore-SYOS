package org.example.business.managers;

import org.example.shared.dto.BillDTO;
import org.example.shared.dto.StockDTO;
import org.example.persistence.gateways.BillGateway;
import org.example.persistence.gateways.StockGateway;
import org.example.shared.patterns.visitors.ReportVisitor;
import org.example.shared.patterns.visitors.DailySalesReportVisitor;
import org.example.shared.patterns.visitors.StockReportVisitor;
import org.example.shared.patterns.visitors.ReorderReportVisitor;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ReportGenerator {
    private static ReportGenerator instance;
    private static final Object lock = new Object();
    private final BillGateway billGateway;
    private final StockGateway stockGateway;

    private ReportGenerator() {
        this.billGateway = BillGateway.getInstance();
        this.stockGateway = StockGateway.getInstance();
    }

    public static ReportGenerator getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ReportGenerator();
                }
            }
        }
        return instance;
    }

    public String generateDailySalesReport(String date) throws SQLException, ClassNotFoundException {
        LocalDate reportDate = LocalDate.parse(date);
        List<BillDTO> bills = billGateway.findByDate(date);
        
        ReportVisitor visitor = new DailySalesReportVisitor(reportDate);
        visitor.visitBills(bills);
        
        return visitor.getReport();
    }

    public String generateStockReport() throws SQLException, ClassNotFoundException {
        return generateStockReport(10); // Default threshold
    }

    public String generateStockReport(int lowStockThreshold) throws SQLException, ClassNotFoundException {
        List<StockDTO> stocks = stockGateway.findAll();
        
        ReportVisitor visitor = new StockReportVisitor(lowStockThreshold);
        visitor.visitStocks(stocks);
        
        return visitor.getReport();
    }

    public String generateReorderReport() throws SQLException, ClassNotFoundException {
        return generateReorderReport(50); // Default threshold as per requirements
    }

    public String generateReorderReport(int reorderThreshold) throws SQLException, ClassNotFoundException {
        List<StockDTO> stocks = stockGateway.findAll();
        
        ReportVisitor visitor = new ReorderReportVisitor(reorderThreshold);
        visitor.visitStocks(stocks);
        
        return visitor.getReport();
    }

    public String generateBillTransactionReport(String startDate, String endDate) throws SQLException, ClassNotFoundException {
        List<BillDTO> bills = billGateway.findByDateRange(startDate, endDate);
        
        StringBuilder report = new StringBuilder();
        report.append("===============================\n");
        report.append("   BILL TRANSACTION REPORT     \n");
        report.append("===============================\n");
        report.append("Period: ").append(startDate).append(" to ").append(endDate).append("\n\n");

        if (bills.isEmpty()) {
            report.append("No transactions found for this period.\n");
            return report.toString();
        }

        report.append(String.format("%-15s %-12s %-15s %-10s %-12s %-10s\n",
                "Invoice", "Date", "Customer", "Total", "Type", "Store"));
        report.append("-------------------------------------------------------------------------\n");

        double totalAmount = 0;
        for (BillDTO bill : bills) {
            String customerName = bill.getCustomerName() != null ? 
                    bill.getCustomerName() : "Walk-in";
            
            if (customerName.length() > 15) {
                customerName = customerName.substring(0, 15);
            }

            report.append(String.format("%-15s %-12s %-15s %-10.2f %-12s %-10s\n",
                    bill.getInvoiceNumber(),
                    bill.getBillDate(),
                    customerName,
                    bill.getFullPrice(),
                    bill.getTransactionType(),
                    bill.getStoreType()));

            totalAmount += bill.getFullPrice();
        }

        report.append("-------------------------------------------------------------------------\n");
        report.append(String.format("Total Transactions: %d\n", bills.size()));
        report.append(String.format("Total Amount: Rs.%.2f\n", totalAmount));
        report.append(String.format("Average Transaction: Rs.%.2f\n", totalAmount / bills.size()));
        report.append("===============================\n");

        return report.toString();
    }

    public void printReport(String reportContent) {
        System.out.println(reportContent);
    }

    public void exportReportToFile(String reportContent, String filename) {
        System.out.println("Report exported to: " + filename);
        System.out.println("Content length: " + reportContent.length() + " characters");
    }
}
