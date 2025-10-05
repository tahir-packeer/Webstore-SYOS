package org.example.Visitor;

import org.example.DTO.BillDTO;
import org.example.DTO.ItemDTO;
import org.example.DTO.StockDTO;

import java.util.List;

public interface ReportVisitor {
    void visitBills(List<BillDTO> bills);
    void visitItems(List<ItemDTO> items);
    void visitStocks(List<StockDTO> stocks);
    String getReport();
}
