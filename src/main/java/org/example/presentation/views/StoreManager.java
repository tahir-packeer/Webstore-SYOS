package org.example.presentation.views;

import org.example.presentation.controllers.Authentication;
import org.example.presentation.controllers.ItemController;
import org.example.presentation.controllers.StockController;
import org.example.persistence.models.Item;
import org.example.persistence.models.Stock;
import org.example.persistence.models.User;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class StoreManager {

    Scanner scanner = new Scanner(System.in);

    private void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[2J\033[H");
            }
        } catch (Exception e) {
            for (int i = 0; i < 30; i++) {
                System.out.println();
            }
        }
    }

    public void storeManagerDashboard(User user) throws SQLException, ClassNotFoundException, ParseException {
        clearScreen();
        displayManagerBanner(user);

        System.out.println("............................................................");
        System.out.println("             MANAGEMENT CONTROL CENTER               ");
        System.out.println("............................................................");
        System.out.println("  [1] Product Inventory Overview                      ");
        System.out.println("  [2] Add New Items                           ");
        System.out.println("  [3] Update Items                      ");
        System.out.println("  [4] Add Stock                               ");
        System.out.println("  [5] Shelf Management                     ");
        System.out.println("  [6] Logout System                           ");
        System.out.println("............................................................");
        System.out.print("Select management function: ");

        int choice = scanner.nextInt();
        scanner.nextLine();

        switch (choice) {
            case 1:
                viewItems(user);
                break;
            case 2:
                addItem_Interface(user);
                break;
            case 3:
                updateItem_Interface(user);
                break;
            case 4:
                addStock_Interface(user);
                break;
            case 5:
                new ShelfManagement().shelf_management_Interface(user);
                break;
            case 6:
                System.out.println(" Initiating secure logout...");
                Authentication.startLoginProcess();
                break;
            default:
                System.out.println(" Invalid selection. Please choose from the available options.");
                storeManagerDashboard(user);
        }
    }

    private void displayManagerBanner(User user) {
        System.out.println("................................................................");
        System.out.println("                        Manager DASHBOARD                    ");
        System.out.println("                                                               ");
        System.out.println("  Manager: " + String.format("%-47s", user.getName()) + " ");
        System.out.println("  Access Level: Store Operations Manager                       ");
        System.out.println("  Session: Active • Terminal: MGR-001                          ");
        System.out.println("                                                               ");
        System.out.println("................................................................");
        System.out.println();
    }

    public void viewItems(User user) throws SQLException, ClassNotFoundException, ParseException {
        System.out.println("\n...................................................");
        System.out.println("               PRODUCT INVENTORY REPORT            ");
        System.out.println("        Complete product catalog overview          ");
        System.out.println("...................................................");

        List<Item> items = new ArrayList<>();
        try {
            items = new ItemController().getAllItems();

            if (items.isEmpty()) {
                System.out.println("\n No products found in inventory.");
                System.out.println(" Consider adding new products to get started.");
            } else {
                System.out.println("\n------------------------------------------------------------");
                System.out.println(" Product ID     Product Name          Price     ");
                System.out.println("------------------------------------------------------------");

                for (Item item : items) {
                    System.out.printf(" %-10s     %-20s     Rs.%-9.2f %n",
                            item.getCode(), item.getName(), item.getPrice());
                }

                System.out.println("------------------------------------------------------------");
                System.out.println("\n Total Products: " + items.size());
            }

        } catch (SQLException | ClassNotFoundException e) {
            System.out.println(" Data retrieval error: " + e.getMessage());
        }

        System.out.println("\n Press Enter to return to dashboard...");
        scanner.nextLine();
        storeManagerDashboard(user);
    }

    public void addItem_Interface(User user) throws SQLException, ClassNotFoundException, ParseException {
        System.out.println("\n...................................................");
        System.out.println("             Add New Items                       ");
        System.out.println("       Add new items to your inventory        ");
        System.out.println("...................................................");

        List<Item> itemList = new ArrayList<Item>();
        String response;

        do {
            System.out.println("\n Enter Product Details:");

            System.out.print("  Item Code : ");
            String code = scanner.nextLine();

            System.out.print(" Item Name : ");
            String name = scanner.nextLine();

            System.out.print(" Item Unit Price : Rs.");
            double price = scanner.nextDouble();
            scanner.nextLine();

            Item item = new Item(code, name, price);
            itemList.add(item);

            System.out.println(" Item '" + name + "' prepared for registration.");

            System.out.println("\n Add another Item? [Y]es / [N]o");
            System.out.print(" Your choice : ");
            response = scanner.nextLine();

        } while (response.equalsIgnoreCase("yes") || response.equalsIgnoreCase("y"));

        try {
            new ItemController().addItems(itemList);
            System.out.println("\n Success! " + itemList.size() + " product(s) registered in inventory!");
        } catch (Exception e) {
            System.out.println(" Registration failed: " + e.getMessage());
        }

        System.out.println("\n Press Enter to continue...");
        scanner.nextLine();
        storeManagerDashboard(user);
    }

    public void updateItem_Interface(User user) throws SQLException, ClassNotFoundException, ParseException {
        System.out.println("\n...................................................");
        System.out.println("            Update Items                         ");
        System.out.println("       Update existing items                     ");
        System.out.println("...................................................");

        System.out.print("\n Enter Product Code to modify : ");
        String itemCode = scanner.nextLine();

        Item searchedItem = new ItemController().getItemFromCode(itemCode);

        if (searchedItem != null) {
            System.out.println("\n Current Item Information:");
            System.out.println("-----------------------------------------");
            System.out.println("  Code: " + String.format("%-30s", searchedItem.getCode()) + " ");
            System.out.println("  Name: " + String.format("%-30s", searchedItem.getName()) + " ");
            System.out.println("  Price: Rs." + String.format("%-26.2f", searchedItem.getPrice()) + " ");
            System.out.println("-----------------------------------------");

            System.out.print("\n New Item Name (Enter to keep existing name) : ");
            String newName = scanner.nextLine();
            if (!newName.isEmpty()) {
                searchedItem.setName(newName);
                System.out.println(" Name updated to: " + newName);
            }

            System.out.print(" New Price (Enter to keep existing price) : Rs.");
            String priceInput = scanner.nextLine();
            if (!priceInput.isEmpty()) {
                try {
                    double newPrice = Double.parseDouble(priceInput);
                    searchedItem.setPrice(newPrice);
                    System.out.println(" Price updated to: Rs." + newPrice);
                } catch (NumberFormatException e) {
                    System.out.println("  Invalid price format. Keeping current price.");
                }
            }

            try {
                new ItemController().updateItem(searchedItem);
                System.out.println("\n Item information updated successfully!");
            } catch (Exception e) {
                System.out.println(" Update failed: " + e.getMessage());
            }

        } else {
            System.out.println("\n Item with code '" + itemCode + "' not found in inventory.");
            System.out.println(" Please verify the item code and try again.");
        }

        System.out.println("\n Press Enter to continue...");
        scanner.nextLine();
        storeManagerDashboard(user);
    }

    public void addStock_Interface(User user) throws SQLException, ClassNotFoundException, ParseException {
        System.out.println("\n...................................................");
        System.out.println("                  Add Stock                      ");
        System.out.println("          Add stock to existing Items            ");
        System.out.println("...................................................");

        System.out.print("\n Enter Item Code for stock update : ");
        String itemCode = scanner.nextLine();

        Item item = new ItemController().getItemFromCode(itemCode);
        if (item != null) {
            System.out.println("\n Item Identified:");
            System.out.println("-----------------------------------------");
            System.out.println("  Item: " + String.format("%-28s", item.getName()) + " ");
            System.out.println("  Code: " + String.format("%-31s", item.getCode()) + " ");
            System.out.println("  Unit Price: Rs." + String.format("%-23.2f", item.getPrice()) + " ");
            System.out.println("-----------------------------------------");

            System.out.print("\n Confirm this item for stock update? [Y]es/[N]o : ");
            String confirmation = scanner.nextLine();

            if (!confirmation.equalsIgnoreCase("yes") && !confirmation.equalsIgnoreCase("y")) {
                System.out.println(" Item not confirmed. Operation cancelled.");
                System.out.println("\n Press Enter to continue...");
                scanner.nextLine();
                storeManagerDashboard(user);
                return;
            }

            System.out.print(" Enter stock quantity to add : ");
            int quantity = scanner.nextInt();
            scanner.nextLine();

            System.out.print(" Enter expiry date (dd/mm/yyyy) : ");
            String expiryDate = scanner.nextLine();

            try {
                Date dateOfExpiry = new java.text.SimpleDateFormat("dd/MM/yyyy").parse(expiryDate);
                Stock currentStock = new Stock(item, quantity, dateOfExpiry);

                new StockController().add_items_to_stock(currentStock);

                System.out.println("\n Stock added successfully!");
                System.out.println(" Added " + quantity + " units of " + item.getName());
                System.out.println(" Expiry Date: " + expiryDate);

            } catch (Exception e) {
                System.out.println(" Stock update failed: " + e.getMessage());
            }

        } else {
            System.out.println("\n Item with code '" + itemCode + "' not found.");
            System.out.println(" Please verify the item code and try again.");
        }

        System.out.println("\n Press Enter to continue...");
        scanner.nextLine();
        storeManagerDashboard(user);
    }

}
