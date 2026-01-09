package org.example.presentation.controllers;

import org.example.business.validators.ReportValidator;

import org.example.persistence.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;

public class ReportController {

    public String generate_sales_report(String date) throws SQLException, ClassNotFoundException {
        return generate_sales_report(date, null, null);
    }

    public String generate_sales_report(String date, String transactionType, String storeType) throws SQLException, ClassNotFoundException {
        
        date = ReportValidator.validateAndNormalizeDate(date);
        
        String[] normalizedFilters = ReportValidator.validateAndNormalizeReportFilters(transactionType, storeType);
        transactionType = normalizedFilters[0];
        storeType = normalizedFilters[1];
        
        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();

        String query = "SELECT i.code, i.name, SUM(bi.quantity) as total_quantity, SUM(bi.totalPrice) as total_revenue " +
                "FROM billItem bi " +
                "JOIN items i ON bi.item_id = i.id " +
                "JOIN bill b ON bi.bill_id = b.id " +
                "WHERE b.billDate = ? ";
        
        if (transactionType != null) {
            query += "AND b.transactionType = ? ";
        }
        if (storeType != null) {
            query += "AND b.storeType = ? ";
        }
        
        query += "GROUP BY i.code, i.name " +
                "ORDER BY total_revenue DESC";

        try (var statement = connection.prepareStatement(query)) {
            statement.setString(1, date);
            int paramIndex = 2;
            if (transactionType != null) {
                statement.setString(paramIndex++, transactionType);
            }
            if (storeType != null) {
                statement.setString(paramIndex, storeType);
            }
            
            System.out.println("\n=== SALES REPORT FOR " + date + " ===");
            System.out.println("Filter: " + ReportValidator.getReportDescription(transactionType, storeType));
            System.out.println("========================================");
            
            var resultSet = statement.executeQuery();

            String reportTitle = "Sales Report for " + date;
            if (transactionType != null) reportTitle += " (" + transactionType + ")";
            if (storeType != null) reportTitle += " [" + storeType + "]";
            
            System.out.println(reportTitle + ":");
            System.out.printf("%-10s %-20s %-15s %-15s%n", "Code", "Item Name", "Total Quantity", "Total Revenue");
            System.out.println("------------------------------------------------------------");

            while (resultSet.next()) {
                String code = resultSet.getString("code");
                String name = resultSet.getString("name");
                int totalQuantity = resultSet.getInt("total_quantity");
                double totalRevenue = resultSet.getDouble("total_revenue");

                System.out.printf("%-10s %-20s %-15d Rs.%-14.2f%n", code, name, totalQuantity, totalRevenue);
            }
        } catch (SQLException e) {
        }
        return query;
    }

    public void generate_items_need_shelving_report() throws SQLException, ClassNotFoundException {
        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();

        String query = """
            SELECT s.id AS shelf_id, i.code AS item_code, i.name AS item_name, s.quantity AS shelf_quantity, s.type,
                   COALESCE(SUM(st.quantity), 0) AS total_stock_quantity
            FROM shelf s
            JOIN items i ON s.item_id = i.id
            LEFT JOIN stock st ON s.item_id = st.item_id
            GROUP BY s.id, s.item_id, i.name, i.code, s.quantity, s.type
            HAVING shelf_quantity < 50 AND total_stock_quantity > 0
            ORDER BY shelf_quantity ASC;
        """;

        try (var statement = connection.prepareStatement(query);
             var resultSet = statement.executeQuery()) {

            System.out.println("Items that need shelving:");
            System.out.printf("%-10s %-20s %-15s %-15s %-15s%n", "Shelf ID", "Item Code", "Item Name", "Shelf Quantity", "Type");
            System.out.println("------------------------------------------------------------");

            while (resultSet.next()) {
                int shelfId = resultSet.getInt("shelf_id");
                String itemCode = resultSet.getString("item_code");
                String itemName = resultSet.getString("item_name");
                int shelfQuantity = resultSet.getInt("shelf_quantity");
                String type = resultSet.getString("type");

                System.out.printf("%-10d %-20s %-15s %-15d %-15s%n", shelfId, itemCode, itemName, shelfQuantity, type);
            }
        } catch (SQLException e) {
        }
    }

    public void generate_reorder_level_report() throws SQLException, ClassNotFoundException {
        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();

        String query = """
            SELECT i.code AS item_code, i.name AS item_name, 
                   COALESCE(SUM(s.quantity), 0) AS total_stock_quantity
            FROM items i
            LEFT JOIN stock s ON i.id = s.item_id AND s.quantity > 0
            GROUP BY i.id, i.code, i.name
            HAVING total_stock_quantity < 50
            ORDER BY total_stock_quantity ASC;
        """;

        try (var statement = connection.prepareStatement(query);
             var resultSet = statement.executeQuery()) {

            System.out.println("Reorder Level Report (Items with stock < 50):");
            System.out.printf("%-10s %-20s %-15s%n", "Item Code", "Item Name", "Current Stock");
            System.out.println("------------------------------------------------------------");

            while (resultSet.next()) {
                String itemCode = resultSet.getString("item_code");
                String itemName = resultSet.getString("item_name");
                int totalStockQuantity = resultSet.getInt("total_stock_quantity");

                System.out.printf("%-10s %-20s %-15d%n", itemCode, itemName, totalStockQuantity);
            }
        } catch (SQLException e) {
        }
    }

    public void generate_reorder_stock_report() throws SQLException, ClassNotFoundException {
        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();

        String query = """
            SELECT i.code AS item_code, i.name AS item_name, SUM(s.quantity) AS total_stock_quantity
            FROM stock s
            JOIN items i ON s.item_id = i.id
            GROUP BY i.code, i.name
            ORDER BY total_stock_quantity DESC;
        """;

        try (var statement = connection.prepareStatement(query);
             var resultSet = statement.executeQuery()) {

            System.out.println("Stock Report:");
            System.out.printf("%-10s %-20s %-15s%n", "Item Code", "Item Name", "Total Stock Quantity");
            System.out.println("------------------------------------------------------------");

            while (resultSet.next()) {
                String itemCode = resultSet.getString("item_code");
                String itemName = resultSet.getString("item_name");
                int totalStockQuantity = resultSet.getInt("total_stock_quantity");

                System.out.printf("%-10s %-20s %-15d%n", itemCode, itemName, totalStockQuantity);
            }
        } catch (SQLException e) {
        }
    }

    public void generate_stock_batch_report() throws SQLException, ClassNotFoundException {
        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();

        String query = """
            SELECT s.id AS stock_id, i.code AS item_code, i.name AS item_name, 
                   s.quantity AS stock_quantity, s.date_of_purchase, s.date_of_expiry
            FROM stock s
            JOIN items i ON s.item_id = i.id
            WHERE s.quantity > 0
            ORDER BY i.code ASC, s.date_of_purchase ASC;
        """;

        try (var statement = connection.prepareStatement(query);
             var resultSet = statement.executeQuery()) {

            System.out.println("Stock Batch Report:");
            System.out.printf("%-8s %-12s %-20s %-8s %-12s %-12s%n", 
                            "Stock ID", "Item Code", "Item Name", "Quantity", "Purchase Date", "Expiry Date");
            System.out.println("------------------------------------------------------------------------------");

            while (resultSet.next()) {
                int stockId = resultSet.getInt("stock_id");
                String itemCode = resultSet.getString("item_code");
                String itemName = resultSet.getString("item_name");
                int stockQuantity = resultSet.getInt("stock_quantity");
                String purchaseDate = resultSet.getString("date_of_purchase");
                String expiryDate = resultSet.getString("date_of_expiry");

                System.out.printf("%-8d %-12s %-20s %-8d %-12s %-12s%n", 
                                stockId, itemCode, itemName, stockQuantity, purchaseDate, expiryDate);
            }
        } catch (SQLException e) {
        }
    }

    public void generate_bill_transaction_report(String startDate, String endDate) throws SQLException, ClassNotFoundException {
        generate_bill_transaction_report(startDate, endDate, null, null);
    }

    public void generate_bill_transaction_report(String startDate, String endDate, String transactionType, String storeType) throws SQLException, ClassNotFoundException {
        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();

        String query = """
            SELECT b.id AS bill_id, b.invoiceNumber, b.billDate, b.transactionType, b.storeType,
                   CASE 
                       WHEN b.customer_type = 'ONLINE' THEN oc.name 
                       ELSE c.name 
                   END AS customer_name, 
                   b.fullPrice, b.discount, b.cashTendered, b.changeAmount
            FROM bill b
            LEFT JOIN customers c ON b.customer_id = c.id AND b.customer_type = 'REGULAR'
            LEFT JOIN online_customers oc ON b.customer_id = oc.id AND b.customer_type = 'ONLINE'
            WHERE b.billDate BETWEEN ? AND ?
        """;
        
        if (transactionType != null) {
            query += " AND b.transactionType = ?";
        }
        if (storeType != null) {
            query += " AND b.storeType = ?";
        }
        
        query += " ORDER BY b.billDate ASC, b.id ASC";

        try (var statement = connection.prepareStatement(query)) {
            statement.setString(1, startDate);
            statement.setString(2, endDate);
            
            int paramIndex = 3;
            if (transactionType != null) {
                statement.setString(paramIndex++, transactionType);
            }
            if (storeType != null) {
                statement.setString(paramIndex, storeType);
            }
            
            var resultSet = statement.executeQuery();

            String reportTitle = "Bill Transaction Report from " + startDate + " to " + endDate;
            if (transactionType != null) reportTitle += " (" + transactionType + ")";
            if (storeType != null) reportTitle += " [" + storeType + "]";
            
            System.out.println(reportTitle + ":");
            System.out.printf("%-8s %-12s %-12s %-10s %-8s %-20s %-10s%n", 
                            "Bill ID", "Invoice No", "Date", "Type", "Store", "Customer", "Amount");
            System.out.println("-------------------------------------------------------------------------------------");

            while (resultSet.next()) {
                int billId = resultSet.getInt("bill_id");
                String invoiceNumber = resultSet.getString("invoiceNumber");
                String billDate = resultSet.getString("billDate");
                String txnType = resultSet.getString("transactionType");
                String store = resultSet.getString("storeType");
                String customerName = resultSet.getString("customer_name");
                double fullPrice = resultSet.getDouble("fullPrice");

                System.out.printf("%-8d %-12s %-12s %-10s %-8s %-20s Rs.%-9.2f%n", 
                                billId, invoiceNumber, billDate, txnType, store, 
                                customerName != null ? customerName : "Walk-in", fullPrice);
            }
        } catch (SQLException e) {
        }
    }
}
