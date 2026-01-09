
package org.example.presentation.servlets;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.json.JSONObject;
import org.json.JSONArray;

import org.example.presentation.controllers.BillController;
import org.example.persistence.models.Bill;
import org.example.persistence.database.DatabaseConnection;

public class ReportServlet extends HttpServlet {
    // Handles reporting operations
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            String type = req.getParameter("type");
            if (type == null)
                type = "sales";

            JSONObject result = new JSONObject();

            switch (type) {
                case "daily-sales":
                    result = handleDailySalesReport(req);
                    break;
                case "items-need-shelving":
                    result = handleItemsNeedShelvingReport(req);
                    break;
                case "reorder-level":
                    result = handleReorderLevelReport(req);
                    break;
                case "stock-report":
                    result = handleStockReport(req);
                    break;
                case "stock-batch":
                    result = handleStockBatchReport(req);
                    break;
                case "bill-transaction":
                    result = handleBillTransactionReport(req);
                    break;
                case "combined-transaction":
                    result = handleCombinedTransactionReport(req);
                    break;
                case "combined-store":
                    result = handleCombinedStoreReport(req);
                    break;
                case "sales":
                default:
                    result = handleBasicSalesReport(req);
                    break;
            }

            resp.getWriter().write(result.toString());
        } catch (Exception e) {
            System.err.println("Error in ReportServlet: " + e.getMessage());
            resp.setStatus(500);
            JSONObject error = new JSONObject();
            error.put("error", e.getMessage());
            error.put("type", e.getClass().getSimpleName());
            resp.getWriter().write(error.toString());
        }
    }

    private JSONObject handleDailySalesReport(HttpServletRequest req) throws SQLException, ClassNotFoundException {
        String date = req.getParameter("date");
        String transactionType = req.getParameter("transactionType");
        String storeType = req.getParameter("storeType");

        JSONObject result = new JSONObject();
        JSONArray data = new JSONArray();

        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();

        String query = "SELECT b.id, b.invoiceNumber, b.fullPrice, b.discount, b.cashTendered, " +
                "b.changeAmount, b.billDate, b.customer_type, b.transactionType, b.storeType, " +
                "COUNT(bi.id) as item_count " +
                "FROM bill b " +
                "LEFT JOIN billItem bi ON b.id = bi.bill_id " +
                "WHERE DATE(b.billDate) = ? ";

        if (transactionType != null && !transactionType.isEmpty()) {
            query += "AND b.transactionType = ? ";
        }
        if (storeType != null && !storeType.isEmpty()) {
            query += "AND b.storeType = ? ";
        }

        query += "GROUP BY b.id ORDER BY b.billDate DESC";

        double totalSales = 0;
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            int paramIndex = 1;
            stmt.setString(paramIndex++, date);
            if (transactionType != null && !transactionType.isEmpty()) {
                stmt.setString(paramIndex++, transactionType);
            }
            if (storeType != null && !storeType.isEmpty()) {
                stmt.setString(paramIndex++, storeType);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    JSONObject bill = new JSONObject();
                    bill.put("id", rs.getInt("id"));
                    bill.put("invoiceNumber", rs.getString("invoiceNumber"));
                    bill.put("fullPrice", rs.getDouble("fullPrice"));
                    bill.put("discount", rs.getDouble("discount"));
                    bill.put("cashTendered", rs.getDouble("cashTendered"));
                    bill.put("changeAmount", rs.getDouble("changeAmount"));
                    bill.put("billDate", rs.getString("billDate"));
                    bill.put("customerType", rs.getString("customer_type"));
                    bill.put("transactionType", rs.getString("transactionType"));
                    bill.put("storeType", rs.getString("storeType"));
                    bill.put("itemCount", rs.getInt("item_count"));
                    data.put(bill);
                    totalSales += rs.getDouble("fullPrice");
                }
            }
        }

        result.put("data", data);
        result.put("summary", new JSONObject().put("totalSales", totalSales).put("totalTransactions", data.length()));
        return result;
    }

    private JSONObject handleItemsNeedShelvingReport(HttpServletRequest req)
            throws SQLException, ClassNotFoundException {
        JSONObject result = new JSONObject();
        JSONArray data = new JSONArray();

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
                    ORDER BY shelf_quantity ASC
                """;

        try (PreparedStatement stmt = connection.prepareStatement(query);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                JSONObject item = new JSONObject();
                item.put("shelfId", rs.getInt("shelf_id"));
                item.put("itemCode", rs.getString("item_code"));
                item.put("itemName", rs.getString("item_name"));
                item.put("shelfQuantity", rs.getInt("shelf_quantity"));
                item.put("type", rs.getString("type"));
                item.put("totalStockQuantity", rs.getInt("total_stock_quantity"));
                data.put(item);
            }
        }

        result.put("data", data);
        result.put("summary", new JSONObject().put("itemsNeedingShelving", data.length()));
        return result;
    }

    private JSONObject handleReorderLevelReport(HttpServletRequest req) throws SQLException, ClassNotFoundException {
        JSONObject result = new JSONObject();
        JSONArray data = new JSONArray();

        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();

        // Use fixed reorder level of 50 (hardcoded business rule: if total stock < 50,
        // reorder needed)
        String query = """
                    SELECT i.id, i.code, i.name, 50 AS reorder_level,
                           COALESCE(SUM(st.quantity), 0) AS total_quantity
                    FROM items i
                    LEFT JOIN stock st ON i.id = st.item_id
                    GROUP BY i.id, i.code, i.name
                    HAVING total_quantity < 50
                    ORDER BY total_quantity ASC
                """;

        try (PreparedStatement stmt = connection.prepareStatement(query);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                JSONObject item = new JSONObject();
                item.put("itemId", rs.getInt("id"));
                item.put("itemCode", rs.getString("code"));
                item.put("itemName", rs.getString("name"));
                item.put("reorderLevel", 50); // Fixed reorder level
                item.put("currentStock", rs.getInt("total_quantity"));
                item.put("shortfall", 50 - rs.getInt("total_quantity"));
                data.put(item);
            }
        }

        result.put("data", data);
        result.put("summary", new JSONObject().put("itemsBelowReorderLevel", data.length()));
        return result;
    }

    private JSONObject handleStockReport(HttpServletRequest req) throws SQLException, ClassNotFoundException {
        JSONObject result = new JSONObject();
        JSONArray data = new JSONArray();

        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();

        String query = """
                    SELECT i.id, i.code, i.name, COALESCE(SUM(st.quantity), 0) AS total_quantity,
                           COUNT(st.id) AS batch_count,
                           MIN(st.date_of_expiry) AS earliest_expiry,
                           MAX(st.date_of_purchase) AS latest_purchase
                    FROM items i
                    LEFT JOIN stock st ON i.id = st.item_id
                    GROUP BY i.id, i.code, i.name
                    ORDER BY i.name
                """;

        try (PreparedStatement stmt = connection.prepareStatement(query);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                JSONObject item = new JSONObject();
                item.put("itemId", rs.getInt("id"));
                item.put("itemCode", rs.getString("code"));
                item.put("itemName", rs.getString("name"));
                item.put("totalQuantity", rs.getInt("total_quantity"));
                item.put("batchCount", rs.getInt("batch_count"));
                item.put("earliestExpiry", rs.getString("earliest_expiry"));
                item.put("latestPurchase", rs.getString("latest_purchase"));
                data.put(item);
            }
        }

        result.put("data", data);
        result.put("summary", new JSONObject().put("totalItems", data.length()));
        return result;
    }

    private JSONObject handleStockBatchReport(HttpServletRequest req) throws SQLException, ClassNotFoundException {
        JSONObject result = new JSONObject();
        JSONArray data = new JSONArray();

        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();

        String query = """
                    SELECT st.id, i.code, i.name, st.quantity, st.date_of_expiry,
                           st.date_of_purchase, st.availability,
                           DATEDIFF(st.date_of_expiry, CURDATE()) AS days_to_expiry
                    FROM stock st
                    JOIN items i ON st.item_id = i.id
                    ORDER BY st.date_of_expiry ASC
                """;

        try (PreparedStatement stmt = connection.prepareStatement(query);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                JSONObject batch = new JSONObject();
                batch.put("stockId", rs.getInt("id"));
                batch.put("itemCode", rs.getString("code"));
                batch.put("itemName", rs.getString("name"));
                batch.put("quantity", rs.getInt("quantity"));
                batch.put("expiryDate", rs.getString("date_of_expiry"));
                batch.put("purchaseDate", rs.getString("date_of_purchase"));
                batch.put("availability", rs.getBoolean("availability"));
                batch.put("daysToExpiry", rs.getInt("days_to_expiry"));
                data.put(batch);
            }
        }

        result.put("data", data);
        result.put("summary", new JSONObject().put("totalBatches", data.length()));
        return result;
    }

    private JSONObject handleBillTransactionReport(HttpServletRequest req) throws SQLException, ClassNotFoundException {
        String startDate = req.getParameter("startDate");
        String endDate = req.getParameter("endDate");
        String transactionType = req.getParameter("transactionType");
        String storeType = req.getParameter("storeType");

        JSONObject result = new JSONObject();
        JSONArray data = new JSONArray();

        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();

        String query = "SELECT b.id, b.invoiceNumber, b.fullPrice, b.discount, b.cashTendered, " +
                "b.changeAmount, b.billDate, b.customer_type, b.transactionType, b.storeType, " +
                "COUNT(bi.id) as item_count, SUM(bi.quantity) as total_items " +
                "FROM bill b " +
                "LEFT JOIN billItem bi ON b.id = bi.bill_id " +
                "WHERE b.billDate BETWEEN ? AND ? ";

        if (transactionType != null && !transactionType.isEmpty()) {
            query += "AND b.transactionType = ? ";
        }
        if (storeType != null && !storeType.isEmpty()) {
            query += "AND b.storeType = ? ";
        }

        query += "GROUP BY b.id ORDER BY b.billDate DESC";

        double totalSales = 0;
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            int paramIndex = 1;
            stmt.setString(paramIndex++, startDate);
            stmt.setString(paramIndex++, endDate);
            if (transactionType != null && !transactionType.isEmpty()) {
                stmt.setString(paramIndex++, transactionType);
            }
            if (storeType != null && !storeType.isEmpty()) {
                stmt.setString(paramIndex++, storeType);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    JSONObject bill = new JSONObject();
                    bill.put("id", rs.getInt("id"));
                    bill.put("invoiceNumber", rs.getString("invoiceNumber"));
                    bill.put("fullPrice", rs.getDouble("fullPrice"));
                    bill.put("discount", rs.getDouble("discount"));
                    bill.put("cashTendered", rs.getDouble("cashTendered"));
                    bill.put("changeAmount", rs.getDouble("changeAmount"));
                    bill.put("billDate", rs.getString("billDate"));
                    bill.put("customerType", rs.getString("customer_type"));
                    bill.put("transactionType", rs.getString("transactionType"));
                    bill.put("storeType", rs.getString("storeType"));
                    bill.put("itemCount", rs.getInt("item_count"));
                    bill.put("totalItems", rs.getInt("total_items"));
                    data.put(bill);
                    totalSales += rs.getDouble("fullPrice");
                }
            }
        }

        result.put("data", data);
        result.put("summary", new JSONObject().put("totalSales", totalSales).put("totalTransactions", data.length()));
        return result;
    }

    private JSONObject handleCombinedTransactionReport(HttpServletRequest req)
            throws SQLException, ClassNotFoundException {
        String date = req.getParameter("date");

        JSONObject result = new JSONObject();
        JSONArray data = new JSONArray();

        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();

        String query = """
                    SELECT transaction_type, COUNT(*) as transaction_count,
                           SUM(full_price) as total_sales, AVG(full_price) as avg_transaction
                    FROM bill
                    WHERE DATE(bill_date) = ?
                    GROUP BY transaction_type
                    ORDER BY total_sales DESC
                """;

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, date);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    JSONObject type = new JSONObject();
                    type.put("transactionType", rs.getString("transaction_type"));
                    type.put("transactionCount", rs.getInt("transaction_count"));
                    type.put("totalSales", rs.getDouble("total_sales"));
                    type.put("avgTransaction", rs.getDouble("avg_transaction"));
                    data.put(type);
                }
            }
        }

        result.put("data", data);
        result.put("summary", new JSONObject().put("totalTypes", data.length()));
        return result;
    }

    private JSONObject handleCombinedStoreReport(HttpServletRequest req) throws SQLException, ClassNotFoundException {
        String date = req.getParameter("date");

        JSONObject result = new JSONObject();
        JSONArray data = new JSONArray();

        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();

        String query = """
                    SELECT store_type, COUNT(*) as transaction_count,
                           SUM(full_price) as total_sales, AVG(full_price) as avg_transaction
                    FROM bill
                    WHERE DATE(bill_date) = ?
                    GROUP BY store_type
                    ORDER BY total_sales DESC
                """;

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, date);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    JSONObject type = new JSONObject();
                    type.put("storeType", rs.getString("store_type"));
                    type.put("transactionCount", rs.getInt("transaction_count"));
                    type.put("totalSales", rs.getDouble("total_sales"));
                    type.put("avgTransaction", rs.getDouble("avg_transaction"));
                    data.put(type);
                }
            }
        }

        result.put("data", data);
        result.put("summary", new JSONObject().put("totalTypes", data.length()));
        return result;
    }

    private JSONObject handleBasicSalesReport(HttpServletRequest req) throws SQLException, ClassNotFoundException {
        JSONObject result = new JSONObject();
        JSONArray data = new JSONArray();

        BillController billController = new BillController();
        java.util.List<Bill> bills = billController.getAllBills();

        for (Bill bill : bills) {
            JSONObject obj = new JSONObject();
            obj.put("id", bill.getId());
            obj.put("invoiceNumber", bill.getInvoiceNumber());
            obj.put("fullPrice", bill.getFullPrice());
            obj.put("discount", bill.getDiscount());
            obj.put("cashTendered", bill.getCashTendered());
            obj.put("changeAmount", bill.getChangeAmount());
            obj.put("billDate", bill.getBillDate());
            obj.put("transactionType", bill.getTransactionType());
            obj.put("storeType", bill.getStoreType());
            data.put(obj);
        }

        result.put("data", data);
        result.put("summary", new JSONObject().put("totalBills", data.length()));
        return result;
    }
}
