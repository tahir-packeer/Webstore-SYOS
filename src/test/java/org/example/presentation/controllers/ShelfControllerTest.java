package org.example.presentation.controllers;

import org.example.persistence.database.DatabaseConnection;
import org.example.persistence.models.Item;
import org.example.persistence.models.Shelf;
import org.example.persistence.models.Shelf_Stock_Information;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ShelfControllerTest {
    private StockController stockController;
    private ShelfController shelfController;
    private DatabaseConnection databaseConnection;
    private ItemController itemController;
    private String uniqueTestId;

    @BeforeEach
    void setUp() throws Exception {
        databaseConnection = DatabaseConnection.getInstance();
        stockController = new StockController();
        shelfController = new ShelfController();
        itemController = new ItemController();

        // Generate unique identifier for this test run
        uniqueTestId = String.valueOf(System.currentTimeMillis() % 100000);

        // Initialize the database connection
        databaseConnection.connect();
    }

    @Test
    void addShelf_Success() throws Exception {
        // Arrange
        String itemCode = "SHF" + uniqueTestId + "01";
        Item item = new Item(itemCode, "Test Shelf Item " + uniqueTestId, 150);
        itemController.addItems(List.of(item));
        Item savedItem = itemController.getItemFromCode(itemCode);

        Shelf shelf = new Shelf(savedItem, 40, "Store");

        // Act
        shelfController.add_items_to_shelf(shelf);

        // Assert
        int shelfId = shelfController.get_latest_added_shelf_id();
        assertTrue(shelfId > 0, "Shelf ID should be greater than 0 after adding a shelf.");
        int stockQuantity = stockController.get_Stock_quantity_by_item(savedItem);
        assertTrue(stockQuantity >= 0, "Stock quantity should be non-negative after adding a shelf.");
        System.out.println("Shelf added successfully with ID: " + shelfId);

    }

    @Test
    void getLatestAddedShelfId_Success() throws Exception {
        // Act
        int shelfId = shelfController.get_latest_added_shelf_id();

        // Assert
        assertTrue(shelfId > 0, "Shelf ID should be greater than 0.");
        System.out.println("Latest added shelf ID: " + shelfId);
    }

    @Test
    void get_all_shelves_Success() throws Exception {
        // Act
        List<Shelf> shelves = shelfController.get_all_shelves();

        // Assert
        assertFalse(shelves.isEmpty(), "There should be at least one shelf in the database.");
        System.out.println("Number of shelves: " + shelves.size());
        for (Shelf shelf : shelves) {
            System.out.println("Shelf ID: " + shelf.getId() + ", Item: " + shelf.getItem().getName() +
                    ", Quantity: " + shelf.getQuantity() + ", Type: " + shelf.getType());
        }
    }

    @Test
    void get_low_stock_shelves_Success() throws Exception {
        // Act
        List<Shelf_Stock_Information> lowStockShelves = shelfController.get_Low_Shelf_With_Stock();

        // Assert
        assertFalse(lowStockShelves.isEmpty(), "There should be at least one low stock shelf in the database.");
        System.out.println("Number of low stock shelves: " + lowStockShelves.size());
        for (Shelf_Stock_Information info : lowStockShelves) {
            System.out.println("Shelf ID: " + info.getShelfId() + ", Item: " + info.getItemName() +
                    ", Quantity: " + info.getShelfQuantity() + ", Type: " + info.getShelfQuantity());
        }
    }

    @Test
    void get_shelf_by_shelf_id_Success() throws Exception {
        // Arrange
        int shelfId = shelfController.get_latest_added_shelf_id();

        // Act
        Shelf shelf = shelfController.get_Shelf_By_Id(shelfId);

        // Assert
        assertNotNull(shelf, "Shelf should not be null for a valid shelf ID.");
        System.out.println("Shelf ID: " + shelf.getId() + ", Item: " + shelf.getItem().getName() +
                ", Quantity: " + shelf.getQuantity() + ", Type: " + shelf.getType());
    }

    @Test
    void get_shelf_by_shelf_id_NotFound() throws Exception {
        // Act
        Shelf shelf = shelfController.get_Shelf_By_Id(-1); // Using an invalid ID

        // Assert
        assertNull(shelf, "Shelf should be null for an invalid shelf ID.");
        System.out.println("No shelf found for the given ID.");
    }

    void restock_Sheld_Success() throws Exception {
        // Arrange
        Item savedItem = itemController.getItemFromCode("15789");
        Shelf shelf = new Shelf(savedItem, 40, "Store");
        shelfController.add_items_to_shelf(shelf);
        int shelfId = shelfController.get_latest_added_shelf_id();

        Shelf existingShelf = shelfController.get_Shelf_By_Id(shelfId);

        // Act
        shelfController.restock_Shelf(existingShelf, 20);

        // Assert
        Shelf updatedShelf = shelfController.get_Shelf_By_Id(shelfId);
        assertNotNull(updatedShelf, "Updated shelf should not be null.");
        assertEquals(60, updatedShelf.getQuantity(), "Shelf quantity should be updated to 60.");
    }

}
