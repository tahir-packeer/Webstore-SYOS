package org.example.View;

import org.example.Controller.Authentication;
import org.example.Controller.ItemController;
import org.example.Controller.StockController;
import org.example.Model.Item;
import org.example.Model.Stock;
import org.example.Model.User;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class StoreManager {

    Scanner scanner = new Scanner(System.in);

    public void storeManagerDashboard(User user) throws SQLException, ClassNotFoundException, ParseException {
        System.out.println("\nWelcome Store Manager: " + user.getName());
        System.out.println("Store Manager Dashboard");
        System.out.println("1. View Items");
        System.out.println("2. Add Item");
        System.out.println("3. Update Item");
        System.out.println("4. Add Stock");
        System.out.println("5. Shelf Management");
        System.out.println("6. Logout");

        int choice = scanner.nextInt();
        scanner.nextLine(); 

        switch (choice) {
            case 1:
                viewItems(user);

            case 2:
                addItem_Interface(user);

            case 3:
                updateItem_Interface(user);
            case 4:
                addStock_Interface(user);
            case 5:
                new ShelfManagement().shelf_management_Interface(user);
            case 6:
                System.out.println("Logging out...");
                Authentication.startLoginProcess();
                break;
            default:
                System.out.println("Invalid choice, please try again.");
                storeManagerDashboard(user);
        }
    }

    public void viewItems(User user) throws SQLException, ClassNotFoundException, ParseException {

        List<Item> items = new ArrayList<>();
        try {
            items = new ItemController().getAllItems();

            System.out.printf("%-10s %-20s %-10s%n", "Item Code", "Item Name", "Price");
            System.out.println("-------------------------------------------------");
            for (Item item : items) {
                System.out.printf("%-10s %-20s Rs.%-10.2f%n", item.getCode(), item.getName(), item.getPrice());
            }

            System.out.println("-------------------------------------------------");




        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("Error fetching items: " + e.getMessage());
        }

        storeManagerDashboard(user);
    }

    public void addItem_Interface(User user) throws SQLException, ClassNotFoundException, ParseException {

        List<Item> itemList = new ArrayList<Item>();
        String response;
        do {
            System.out.print("Enter item code: ");
            String code = scanner.nextLine();

            System.out.print("Enter item name: ");
            String name = scanner.nextLine();

            System.out.print("Enter item price: Rs.");
            double price = scanner.nextDouble();
            scanner.nextLine(); // Consume newline

            Item item = new Item(code, name, price);

            itemList.add(item);

            System.out.println("Do you want to add another item? (yes/no)");
            response = scanner.nextLine();

        } while (response.equalsIgnoreCase("yes") || response.equalsIgnoreCase("y"));

        new ItemController().addItems(itemList);

        storeManagerDashboard(user);
    }

    public void updateItem_Interface(User user) throws SQLException, ClassNotFoundException, ParseException {

        System.out.print("Enter the Item Code to update: ");
        String itemCode = scanner.nextLine();

        Item searchedItem = new ItemController().getItemFromCode(itemCode);

        if (searchedItem != null) {
            System.out.println("Current Item Details:");
            System.out.println("Code: " + searchedItem.getCode());
            System.out.println("Name: " + searchedItem.getName());
            System.out.println("Price: Rs." + searchedItem.getPrice());

            System.out.print("Enter new name (or press Enter to keep current): ");
            String newName = scanner.nextLine();
            if (!newName.isEmpty()) {
                searchedItem.setName(newName);
            }

            System.out.print("Enter new price (or press Enter to keep current): Rs.");
            String priceInput = scanner.nextLine();
            if (!priceInput.isEmpty()) {
                double newPrice = Double.parseDouble(priceInput);
                searchedItem.setPrice(newPrice);
            }

           new ItemController().updateItem(searchedItem);

            System.out.println("Item updated successfully.");
            storeManagerDashboard(user);

        } else {
            System.out.println("Item not found.");
        }
    }

    public void addStock_Interface(User user) throws SQLException, ClassNotFoundException, ParseException {
        System.out.println("Add Stock");

        System.out.print("Enter Item Code: ");
        String itemCode = scanner.nextLine();

        Item item = new ItemController().getItemFromCode(itemCode);
        if (item != null) {
            System.out.println("Current Stock for " + item.getName() + ": Rs." + item.getPrice());

            System.out.println("Is this the correct item? (yes/no)");
            String confirmation = scanner.nextLine();

            if (!confirmation.equalsIgnoreCase("yes")) {
                
                System.out.println("Item not confirmed. Returning to dashboard.");
                addItem_Interface(user);
                return;
            }

            System.out.print("Enter new stock quantity: ");
            int quantity = scanner.nextInt();
            scanner.nextLine();

            System.out.print("Enter the Date of Expiry (dd/mm/yyyy): ");
            String expiryDate = scanner.nextLine();

            Date dateOfExpiry = new java.text.SimpleDateFormat("dd/MM/yyyy").parse(expiryDate);

            Stock currentStock = new Stock(item, quantity, dateOfExpiry);

            new StockController().add_items_to_stock(currentStock);

            storeManagerDashboard(user);

        } else {
            System.out.println("Item not found.");
        }
    }




}
