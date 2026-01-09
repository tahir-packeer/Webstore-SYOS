package org.example.presentation.views;

import org.example.presentation.controllers.ItemController;
import org.example.presentation.controllers.ShelfController;
import org.example.presentation.controllers.StockController;
import org.example.persistence.models.Item;
import org.example.persistence.models.Shelf;
import org.example.persistence.models.Shelf_Stock_Information;
import org.example.persistence.models.User;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;
import java.util.Scanner;

public class ShelfManagement {

    Scanner scanner = new Scanner(System.in);

    public void shelf_management_Interface(User user)
            throws SQLException, ClassNotFoundException, ParseException {
        System.out.println("\n...................................................");
        System.out.println("            SHELF MANAGEMENT SYSTEM             ");
        System.out.println("...................................................");
        System.out.println("  [1] Add Shelf");
        System.out.println("  [2] View Shelves");
        System.out.println("  [3] Restock Shelf");
        System.out.println("  [4] Back to Dashboard");
        System.out.println("...................................................");
        System.out.print("Select your choice: ");

        int choice = scanner.nextInt();
        scanner.nextLine();

        switch (choice) {
            case 1:
                addShelf_Interface(user);
                break;
            case 2:
                viewShelves_Interface(user);
                break;
            case 3:
                restock_Shelf_Interface(user);
                break;
            case 4:
                System.out.println(" Returning to Dashboard...");
                new StoreManager().storeManagerDashboard(user);
                break;
            default:
                System.out.println(" Invalid choice, please try again.");
                shelf_management_Interface(user);
        }
    }

    public void addShelf_Interface(User user)
            throws SQLException, ClassNotFoundException, ParseException {
        System.out.println("\n...................................................");
        System.out.println("             ADD SHELF INTERFACE               ");
        System.out.println("...................................................");

        System.out.print("Enter the Item Code: ");
        String itemCode = scanner.nextLine();

        Item selectedItem = new ItemController().getItemFromCode(itemCode);
        int totalQuantity = new StockController().get_Stock_quantity_by_item(selectedItem);
        if (selectedItem != null) {
            System.out.println("\n Item Details:");
            System.out.println("------------------------------------------------------------");
            System.out.println(" Code: " + selectedItem.getCode() + " | Name: " + selectedItem.getName()
                    + " | Price: Rs." + selectedItem.getPrice() + " | Stock: " + totalQuantity);
            System.out.println("------------------------------------------------------------");

            System.out.print("Enter the Quantity to add to the shelf: ");
            int quantity = scanner.nextInt();

            if (quantity > totalQuantity) {
                System.out.println(" Quantity exceeds available stock. Please enter a valid quantity.");
                addShelf_Interface(user);
                return;
            } else if (quantity <= 0) {
                System.out.println(" Invalid quantity. Please enter a positive number.");
                addShelf_Interface(user);
                return;
            }

            System.out.println("Select the Shelf Type (Website/Store):");
            String shelfType = null;
            while (shelfType == null || shelfType.isEmpty()) {
                shelfType = scanner.nextLine();
            }

            if (shelfType.equalsIgnoreCase("Website") || shelfType.equalsIgnoreCase("Store")) {

                Shelf shelf = new Shelf(
                        selectedItem,
                        quantity,
                        shelfType);

                new ShelfController().add_items_to_shelf(shelf);

                shelf_management_Interface(user);

            } else {
                System.out.println(" Invalid Shelf Type. Please enter 'Website' or 'Store'.");
                addShelf_Interface(user);
            }

        } else {
            System.out.println(" Invalid Item Code Please try again.");
        }
    }

    public void viewShelves_Interface(User user)
            throws SQLException, ClassNotFoundException, ParseException {

        System.out.println("\n...................................................");
        System.out.println("             VIEW SHELVES REPORT               ");
        System.out.println("...................................................");

        List<Shelf> shelves = new ShelfController().get_all_shelves();

        if (shelves.isEmpty()) {
            System.out.println(" Shelves are empty.");
        } else {
            System.out.printf("%-5s %-20s %-10s %-10s%n", "ID", "Item Name", "Quantity", "Type");
            System.out.println("------------------------------------------------------------");

            for (Shelf shelf : shelves) {
                Item item = shelf.getItem();
                String itemName = (item != null) ? item.getName() : "Unknown";

                System.out.printf("%-5d %-20s %-10d %-10s%n",
                        shelf.getId(),
                        itemName,
                        shelf.getQuantity(),
                        shelf.getType());
            }
        }

        shelf_management_Interface(user);
    }

    public void restock_Shelf_Interface(User user)
            throws SQLException, ClassNotFoundException, ParseException {
        System.out.println("\n...................................................");
        System.out.println("            RESTOCK SHELF INTERFACE            ");
        System.out.println("...................................................");

        List<Shelf_Stock_Information> shelves = new ShelfController().get_Low_Shelf_With_Stock();

        System.out.printf("%-5s %-10s %-20s %-10s %-10s %-15s%n", "ID", "Item Code", "Item Name", "Shelf Qty", "Type",
                "Stock Qty");
        System.out.println("-------------------------------------------------------------------");

        if (shelves.isEmpty()) {
            System.out.println(" No matching shelves found.");
            return;
        }

        for (Shelf_Stock_Information info : shelves) {
            System.out.printf("%-5d %-10s %-20s %-10d %-10s %-15d%n",
                    info.getShelfId(),
                    info.getItemCode(),
                    info.getItemName(),
                    info.getShelfQuantity(),
                    info.getType(),
                    info.getTotalStockQuantity());
        }

        System.out.print("Enter the Shelf ID to restock: ");
        int shelfId = scanner.nextInt();

        System.out.print("Enter the quantity to restock: ");
        int quantity = scanner.nextInt();

        Shelf shelf = new ShelfController().get_Shelf_By_Id(shelfId);
        if (shelf == null) {
            System.out.println(" Invalid Shelf ID. Please try again.");
            restock_Shelf_Interface(user);
            return;
        }

        if (quantity <= 0) {
            System.out.println(" Invalid quantity. Please enter a positive number.");
            restock_Shelf_Interface(user);
            return;
        }

        int totalStockQuantity = new StockController().get_Stock_quantity_by_item(shelf.getItem());
        if (quantity > totalStockQuantity) {
            System.out.println(" Quantity exceeds available stock. Please enter a valid quantity.");
            restock_Shelf_Interface(user);
            return;
        }

        new ShelfController().restock_Shelf(shelf, quantity);

        shelf_management_Interface(user);
    }
}
