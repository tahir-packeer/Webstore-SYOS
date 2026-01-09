package org.example.shared.patterns.visitors;

import org.example.shared.dto.BillDTO;
import org.example.shared.dto.ItemDTO;
import org.example.shared.dto.StockDTO;

import java.util.List;

// Visitor for generating reorder reports
public class ReorderReportVisitor implements ReportVisitor {
    private StringBuilder report;
    private int reorderThreshold;
    
    public ReorderReportVisitor(int reorderThreshold) {
        this.report = new StringBuilder();
        this.reorderThreshold = reorderThreshold;
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
        report.append("       REORDER REPORT          \n");
        report.append("===============================\n");
        report.append("Reorder Threshold: ").append(reorderThreshold).append("\n\n");

        // Filter items that need reordering
        List<StockDTO> reorderItems = stocks.stream()
                .filter(stock -> stock.getQuantity() <= reorderThreshold && stock.isAvailability())
                .toList();

        if (reorderItems.isEmpty()) {
            report.append("No items need reordering at this time.\n");
            report.append("===============================\n");
            return;
        }

        report.append("ITEMS REQUIRING REORDER:\n");
        report.append(String.format("%-10s %-25s %-10s %-15s\n",
                "Code", "Name", "Quantity", "Suggested Order"));
        report.append("---------------------------------------------------------------\n");

        for (StockDTO stock : reorderItems) {
            String itemName = stock.getItemName().length() > 25 ? 
                    stock.getItemName().substring(0, 25) : stock.getItemName();
            
            // Suggest ordering enough to reach 3x the reorder threshold
            int suggestedOrder = (reorderThreshold * 3) - stock.getQuantity();
            
            report.append(String.format("%-10s %-25s %-10d %-15d\n",
                    stock.getItemCode(),
                    itemName,
                    stock.getQuantity(),
                    suggestedOrder));
        }

        report.append("---------------------------------------------------------------\n");
        report.append(String.format("Total Items to Reorder: %d\n", reorderItems.size()));
        
        // Calculate total suggested order quantity
        int totalSuggestedOrder = reorderItems.stream()
                .mapToInt(stock -> (reorderThreshold * 3) - stock.getQuantity())
                .sum();
        
        report.append(String.format("Total Suggested Order Quantity: %d\n", totalSuggestedOrder));
        report.append("===============================\n");
    }

    @Override
    public String getReport() {
        return report.toString();
    }
}
