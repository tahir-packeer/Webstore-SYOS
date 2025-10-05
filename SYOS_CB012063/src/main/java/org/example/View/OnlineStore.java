package org.example.View;

import org.example.Controller.ItemController;
import org.example.Controller.OnlineController;
import org.example.Controller.CustomerController;
import org.example.Model.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class OnlineStore {
    
    private Scanner scanner = new Scanner(System.in);
    private OnlineController onlineController = new OnlineController();
    private ItemController itemController = new ItemController();
    private CustomerController customerController = new CustomerController();

    public void startOnlineStore() throws SQLException, ClassNotFoundException {
        System.out.println("\n=== Welcome to SYOS Online Store ===");
        
        while (true) {
            System.out.println("\n1. Register New Customer");
            System.out.println("2. Place Order");
            System.out.println("3. View Available Items");
            System.out.println("4. Exit");
            System.out.print("Choose an option: ");
            
            int choice = scanner.nextInt();
            scanner.nextLine();
            
            switch (choice) {
                case 1:
                    registerCustomer();
                    break;
                case 2:
                    placeOrder();
                    break;
                case 3:
                    showAvailableItems();
                    break;
                case 4:
                    System.out.println("Thank you for visiting SYOS Online Store!");
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void registerCustomer() throws SQLException, ClassNotFoundException {
        System.out.println("\n=== Customer Registration ===");
        
        System.out.print("Enter your name: ");
        String name = scanner.nextLine();
        
        System.out.print("Enter your phone number: ");
        String phone = scanner.nextLine();
        
        System.out.print("Enter your email: ");
        String email = scanner.nextLine();
        
        System.out.print("Enter your address: ");
        String address = scanner.nextLine();
        
        Customer customer = new Customer(name, phone);
        
        try {
            if (onlineController.registerOnlineCustomer(customer, email, address)) {
                System.out.println("Registration successful! Customer ID: " + customer.getId());
            } else {
                System.out.println("Registration failed. Please try again.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Registration failed due to validation errors:");
            System.out.println(e.getMessage());
            System.out.println("Please check your input and try again.");
        }
    }

    private void placeOrder() throws SQLException, ClassNotFoundException {
        System.out.println("\n=== Place Order ===");
        
        // Get customer
        System.out.print("Enter your phone number: ");
        String phone = scanner.nextLine();
        
        Customer customer = customerController.get_OnlineCustomer_from_contactNumber(phone);
        if (customer == null) {
            System.out.println("Customer not found. Please register first.");
            return;
        }
        
        System.out.println("Welcome, " + customer.getName() + "!");
        
        showAvailableItems();
        
        List<BillItem> orderItems = new ArrayList<>();
        boolean addMoreItems = true;
        
        while (addMoreItems) {
            System.out.print("\nEnter item code (or press Enter to finish): ");
            String itemCode = scanner.nextLine();
            
            if (itemCode.isEmpty()) {
                addMoreItems = false;
                break;
            }
            
            Item item = itemController.getItemFromCode(itemCode);
            if (item == null) {
                System.out.println("Item not found!");
                continue;
            }
            
            System.out.print("Enter quantity: ");
            int quantity = scanner.nextInt();
            scanner.nextLine();
            
            if (onlineController.isItemAvailableOnline(item.getId(), quantity)) {
                BillItem billItem = new BillItem(item, quantity);
                orderItems.add(billItem);
                System.out.println("Added: " + item.getName() + " x " + quantity + 
                                 " = Rs." + billItem.getTotalPrice());
            } else {
                System.out.println("Insufficient stock for this item online!");
            }
        }
        
        if (orderItems.isEmpty()) {
            System.out.println("No items in order.");
            return;
        }
        
        // Calculate total
        double total = orderItems.stream().mapToDouble(BillItem::getTotalPrice).sum();
        System.out.println("\nOrder Total: Rs." + total);
        
        System.out.print("Apply discount (Enter 0 for no discount): Rs.");
        double discount = scanner.nextDouble();
        scanner.nextLine();
        
        // Process order
        try {
            Bill bill = onlineController.processOnlineOrder(orderItems, customer, discount);
            System.out.println("\nOrder processed successfully!");
            System.out.println("Order Number: " + bill.getInvoiceNumber());
            System.out.println("Final Amount: Rs." + bill.getFullPrice());
            System.out.println("Your order will be delivered to your registered address.");
        } catch (SQLException e) {
            System.out.println("Order processing failed: " + e.getMessage());
        }
    }

    private void showAvailableItems() throws SQLException, ClassNotFoundException {
        System.out.println("\n=== Available Items ===");
        List<Item> items = onlineController.getWebsiteInventory();
        
        if (items.isEmpty()) {
            System.out.println("No items available online at the moment.");
            return;
        }
        
        System.out.printf("%-10s %-20s %-10s%n", "Code", "Name", "Price");
        System.out.println("----------------------------------------");
        
        for (Item item : items) {
            System.out.printf("%-10s %-20s Rs.%-9.2f%n", 
                            item.getCode(), item.getName(), item.getPrice());
        }
    }
}
