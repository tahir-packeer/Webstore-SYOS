package org.example.Controller;

import org.example.Database.DatabaseConnection;
import org.example.Model.Bill;
import org.example.Model.BillItem;

import java.sql.*;
import java.util.List;

/**
 * Business Logic Controller for Bill operations
 * Handles database operations for bills and bill items
 * Used by BillApiController (REST API) and other parts of the application
 */
public class BillController {
    public java.util.List<Bill> getAllBills() throws SQLException, ClassNotFoundException {
        java.util.List<Bill> bills = new java.util.ArrayList<>();
        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();
        String query = "SELECT * FROM bill ORDER BY billDate DESC LIMIT 100";
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Bill bill = new Bill(null,
                    rs.getString("invoiceNumber"),
                    rs.getDouble("fullPrice"),
                    rs.getDouble("discount"),
                    rs.getDouble("cashTendered"),
                    rs.getDouble("changeAmount"),
                    rs.getString("transactionType"),
                    rs.getString("storeType")
                );
                bill.setId(rs.getInt("id"));
                bill.setBillDate(rs.getDate("billDate").toLocalDate());
                bills.add(bill);
            }
        }
        return bills;
    }

    public String getInvoiceNumber() throws SQLException, ClassNotFoundException {
        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();
        String query = "SELECT COUNT(*) AS total FROM bill";

        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                int count = rs.getInt("total") + 1;
                return String.format("INV-%05d", count); // Format: INV-00001
            }
        }
        return "INV-00001"; // Default for first invoice
    }

    public Bill Add_Bill(Bill bill) throws SQLException, ClassNotFoundException {
        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();
        PreparedStatement statement = null;
        String query = "INSERT INTO bill (customer_id, customer_type, invoiceNumber, fullPrice, discount, cashTendered,changeAmount,billDate,transactionType,storeType) VALUES (?, ?, ?, ?, ?, ?, ?,?,?,?)";
        statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        if (bill.getCustomer() != null) {
            statement.setInt(1, bill.getCustomer().getId());
        } else {
            statement.setNull(1, Types.INTEGER);
        }
        statement.setString(2, "REGULAR"); // Regular customers
        statement.setString(3, bill.getInvoiceNumber());
        statement.setDouble(4, bill.getFullPrice());
        statement.setDouble(5, bill.getDiscount());
        statement.setDouble(6, bill.getCashTendered());
        statement.setDouble(7, bill.getChangeAmount());
        statement.setObject(8, bill.getBillDate());
        statement.setString(9, bill.getTransactionType());
        statement.setString(10, bill.getStoreType());

        int rowsInserted = statement.executeUpdate();

        if (rowsInserted > 0) {
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    bill.setId(generatedId); // Set the generated ID back to the bill object
                    System.out.println("Bill added successfully with ID: " + generatedId);
                }
            }
        } else {
            // Failed to add bill
        }
        return bill;
    }

    public void add_Bill_items
            (List<BillItem> billItems, Bill bill)
            throws SQLException, ClassNotFoundException
    {
        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection connection = db.connect();
        PreparedStatement Billstatement = null;
        PreparedStatement ShelfStatement = null;

        String BillItemquery = "INSERT INTO billItem ( item_id,bill_id, quantity, itemPrice, totalPrice) VALUES (?, ?, ?, ?,?)";
        String updateShelfQuery = "UPDATE shelf SET quantity = quantity - ? WHERE item_id = ? AND type = ? AND quantity >= ? LIMIT 1";

        Billstatement = connection.prepareStatement(BillItemquery);
        ShelfStatement = connection.prepareStatement(updateShelfQuery);

        // Determine which shelf to update based on transaction type
        String shelfType = bill.getStoreType().equals("WEBSITE") ? "WEBSITE" : "STORE";

        for (BillItem billItem : billItems) {
            // Add bill item record
            Billstatement.setInt(1, billItem.getItem().getId());
            Billstatement.setInt(2, bill.getId());
            Billstatement.setInt(3, billItem.getQuantity());
            Billstatement.setDouble(4, billItem.getItemPrice());
            Billstatement.setDouble(5, billItem.getTotalPrice());

            Billstatement.executeUpdate();

            // Update shelf stock quantity
            ShelfStatement.setInt(1, billItem.getQuantity());
            ShelfStatement.setInt(2, billItem.getItem().getId());
            ShelfStatement.setString(3, shelfType);
            ShelfStatement.setInt(4, billItem.getQuantity());
            ShelfStatement.executeUpdate();
        }

        Billstatement.close();
        ShelfStatement.close();



    }

}
