package org.example.presentation.controllers;

import org.example.persistence.database.DatabaseConnection;
import org.example.persistence.models.Item;
import org.example.persistence.models.Shelf;
import org.example.persistence.models.Shelf_Stock_Information;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ShelfController {

    public void add_items_to_shelf
            (Shelf shelf)
            throws SQLException, ClassNotFoundException
    {
        DatabaseConnection databaseConnection = DatabaseConnection.getInstance();
        Connection connection = databaseConnection.connect();
        PreparedStatement statement = null;

        try {
            // Prepare the SQL statement to insert a new shelf
            statement = connection.prepareStatement("INSERT INTO shelf (item_id, quantity, type) VALUES (?, ?, ?)");
            statement.setInt(1, shelf.getItem().getId());
            statement.setInt(2, shelf.getQuantity());
            statement.setString(3, shelf.getType());
            // Execute the statement
            int rowsInserted = statement.executeUpdate();
            if (rowsInserted > 0) {

                int shelfId = get_latest_added_shelf_id();

                new StockController().reduce_stock_quantity_and_update_stock_shelf_table(shelf.getItem(), shelf.getQuantity(), shelfId);



            } else {
                // Failed to add shelf
            }
        } finally {
            if (statement != null) statement.close();
            databaseConnection.closeConnection(connection);
        }
    }

    public int get_latest_added_shelf_id()
            throws SQLException, ClassNotFoundException
    {
        DatabaseConnection databaseConnection = DatabaseConnection.getInstance();
        Connection connection = databaseConnection.connect();
        PreparedStatement statement = null;

        try {
            // Prepare the SQL statement to get the latest added shelf ID
            statement = connection.prepareStatement("SELECT MAX(id) FROM shelf");
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getInt(1);
            } else {
                System.out.println("No shelves found.");
                return -1; // Return -1 if no shelves exist
            }
        } finally {
            if (statement != null) statement.close();
            databaseConnection.closeConnection(connection);
        }
    }

    public List<Shelf> get_all_shelves()
            throws SQLException, ClassNotFoundException
    {
        DatabaseConnection databaseConnection = DatabaseConnection.getInstance();
        Connection connection = databaseConnection.connect();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<Shelf> shelves = new ArrayList<>();

        try {
            // Prepare the SQL statement to get all shelves
            statement = connection.prepareStatement("SELECT * FROM shelf");
            resultSet = statement.executeQuery();

            while (resultSet.next()) {

                int shelf_id = resultSet.getInt("id");
                Item self_item = new ItemController().getItemFromId(resultSet.getInt("item_id"));
                int shelf_quantity = resultSet.getInt("quantity");
                String type = resultSet.getString("type");
                Shelf shelf = new Shelf(self_item, shelf_quantity, type);

                shelf.setId(shelf_id);
                shelves.add(shelf);
            }

            return shelves;
        } finally {
            if (resultSet != null) resultSet.close();
            if (statement != null) statement.close();
            databaseConnection.closeConnection(connection);
        }
    }

    public List<Shelf_Stock_Information> get_Low_Shelf_With_Stock() throws SQLException, ClassNotFoundException {
        List<Shelf_Stock_Information> resultList = new ArrayList<>();
        DatabaseConnection databaseConnection = DatabaseConnection.getInstance();
        Connection connection = databaseConnection.connect();

        String query = """
            SELECT 
                s.id AS shelf_id,
                s.item_id,
                i.name AS item_name,
                i.code AS item_code,
                s.quantity AS shelf_quantity,
                s.type,
                COALESCE(SUM(st.quantity), 0) AS total_stock_quantity
            FROM shelf s
            JOIN items i ON s.item_id = i.id
            LEFT JOIN stock st ON s.item_id = st.item_id
            GROUP BY s.id, s.item_id, i.name, i.code, s.quantity, s.type
            HAVING shelf_quantity < 50 AND total_stock_quantity > 0
            ORDER BY shelf_quantity ASC;
        """;

        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                Shelf_Stock_Information info = new Shelf_Stock_Information(
                        rs.getInt("shelf_id"),
                        rs.getString("item_code"),
                        rs.getString("item_name"),
                        rs.getInt("shelf_quantity"),
                        rs.getString("type"),
                        rs.getInt("total_stock_quantity")
                );
                resultList.add(info);
            }
        } finally {
            connection.close();
        }

        return resultList;
    }

    public Shelf get_Shelf_By_Id(int shelfId) throws SQLException, ClassNotFoundException {
        DatabaseConnection databaseConnection = DatabaseConnection.getInstance();
        Connection connection = databaseConnection.connect();
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            // Prepare the SQL statement to get a shelf by its ID
            statement = connection.prepareStatement("SELECT * FROM shelf WHERE id = ?");
            statement.setInt(1, shelfId);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                Item item = new ItemController().getItemFromId(resultSet.getInt("item_id"));
                int quantity = resultSet.getInt("quantity");
                String type = resultSet.getString("type");
                Shelf shelf = new Shelf(item, quantity, type);
                shelf.setId(shelfId);
                return shelf;
            } else {
                System.out.println("Shelf with ID " + shelfId + " not found.");
                return null;
            }
        } finally {
            if (resultSet != null) resultSet.close();
            if (statement != null) statement.close();
            databaseConnection.closeConnection(connection);
        }
    }

    public void restock_Shelf(Shelf shelf, int quantity)
            throws SQLException, ClassNotFoundException
    {
        DatabaseConnection databaseConnection = DatabaseConnection.getInstance();
        Connection connection = databaseConnection.connect();

        try
        {
            connection.setAutoCommit(false);

            int quantityToAdd = quantity;

            String stockQuery = """
            SELECT id, quantity FROM stock
            WHERE item_id = ? AND quantity > 0
            ORDER BY date_of_expiry ASC
            """;

            PreparedStatement stockStatement = connection.prepareStatement(stockQuery);
            stockStatement.setInt(1, shelf.getItem().getId());
            ResultSet stockResultSet = stockStatement.executeQuery();

            while (stockResultSet.next() && quantityToAdd > 0) {
                int stockId = stockResultSet.getInt("id");
                int stockQuantity = stockResultSet.getInt("quantity");

                int quantityToReduce = Math.min(quantityToAdd, stockQuantity);

                PreparedStatement updateStockQuery = connection.prepareStatement(
                        "UPDATE stock SET quantity = quantity - ? WHERE id = ?");
                updateStockQuery.setInt(1, quantityToReduce);
                updateStockQuery.setInt(2, stockId);
                updateStockQuery.executeUpdate();
                updateStockQuery.close();

                String insertShelfStock = "INSERT INTO shelf_stock (shelf_id, stock_id, quantity_moved) VALUES (?, ?, ?)";
                PreparedStatement shelfStockStmt = connection.prepareStatement(insertShelfStock);
                shelfStockStmt.setInt(1, shelf.getId());
                shelfStockStmt.setInt(2, stockId);
                shelfStockStmt.setInt(3, quantityToReduce);
                shelfStockStmt.executeUpdate();

                quantityToAdd -= quantityToReduce;
            }

            if (quantityToAdd > 0) {
                connection.rollback();
                throw new SQLException("Not enough stock to restock the shelf.");
            }

            String updateShelf = "UPDATE shelf SET quantity = quantity + ? WHERE id = ?";
            PreparedStatement updateShelfStmt = connection.prepareStatement(updateShelf);
            updateShelfStmt.setInt(1, quantity);
            updateShelfStmt.setInt(2, shelf.getId());
            updateShelfStmt.executeUpdate();

            connection.commit();
            System.out.println("Shelf restocked successfully with " + quantity + " items.");
        }
        catch (SQLException e)
        {
            connection.rollback();
            throw e;
        }
        finally
        {
            connection.setAutoCommit(true);
            connection.close();
        }
    }


}
