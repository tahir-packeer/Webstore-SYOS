package org.example.shared.patterns.visitors;

import org.example.shared.dto.BillDTO;
import org.example.shared.dto.ItemDTO;
import org.example.shared.dto.StockDTO;

import java.util.List;

public class StockReportVisitor implements ReportVisitor {
    private StringBuilder report;
    private int lowStockThreshold;
    
    public StockReportVisitor(int lowStockThreshold) {
        this.report = new StringBuilder();
        this.lowStockThreshold = lowStockThreshold;
    }

    @Override
    public void visitBills(List<BillDTO> bills) {
    }

    @Override
    public void visitItems(List<ItemDTO> items) {
    }

    @Override
    public void visitStocks(List<StockDTO> stocks) {
        report.append("===============================\n");
        report.append("        STOCK REPORT           \n");
        report.append("===============================\n");
        report.append("Low Stock Threshold: ").append(lowStockThreshold).append("\n\n");

        if (stocks.isEmpty()) {
            report.append("No stock items found.\n");
            return;
        }

        // Filter low stock items
        List<StockDTO> lowStockItems = stocks.stream()
                .filter(stock -> stock.getQuantity() <= lowStockThreshold)
                .toList();

        // All stock items
        report.append("ALL STOCK ITEMS:\n");
        report.append(String.format("%-10s %-20s %-10s %-12s %-12s\n",
                "Code", "Name", "Quantity", "Purchase", "Expiry"));
        report.append("---------------------------------------------------------------\n");

        for (StockDTO stock : stocks) {
            String itemName = stock.getItemName().length() > 20 ? 
                    stock.getItemName().substring(0, 20) : stock.getItemName();
            
            report.append(String.format("%-10s %-20s %-10d %-12s %-12s\n",
                    stock.getItemCode(),
                    itemName,
                    stock.getQuantity(),
                    stock.getDateOfPurchase(),
                    stock.getDateOfExpiry()));
        }

        if (!lowStockItems.isEmpty()) {
            report.append("\nLOW STOCK ITEMS (Quantity <= ").append(lowStockThreshold).append("):\n");
            report.append(String.format("%-10s %-20s %-10s\n",
                    "Code", "Name", "Quantity"));
            report.append("---------------------------------------\n");

            for (StockDTO stock : lowStockItems) {
                String itemName = stock.getItemName().length() > 20 ? 
                        stock.getItemName().substring(0, 20) : stock.getItemName();
                
                report.append(String.format("%-10s %-20s %-10d\n",
                        stock.getItemCode(),
                        itemName,
                        stock.getQuantity()));
            }
        }

        report.append("\n===============================\n");
        report.append(String.format("Total Stock Items: %d\n", stocks.size()));
        report.append(String.format("Low Stock Items: %d\n", lowStockItems.size()));
        report.append("===============================\n");
    }

    @Override
    public String getReport() {
        return report.toString();
    }
}
