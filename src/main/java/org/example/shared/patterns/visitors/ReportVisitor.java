package org.example.shared.patterns.visitors;

import org.example.shared.dto.BillDTO;
import org.example.shared.dto.ItemDTO;
import org.example.shared.dto.StockDTO;

import java.util.List;

public interface ReportVisitor {
    void visitBills(List<BillDTO> bills);
    void visitItems(List<ItemDTO> items);
    void visitStocks(List<StockDTO> stocks);
    String getReport();
}
