package org.example.presentation.views;

import org.example.presentation.controllers.ItemController;
import org.example.presentation.controllers.OnlineController;
import org.example.presentation.controllers.CustomerController;
import org.example.persistence.models.*;

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
        clearScreen();
        displayShoppingBanner();

        while (true) {
            System.out.println("........................................................");
            System.out.println("                CUSTOMER Dashboard                   ");
            System.out.println("........................................................");
            System.out.println("  [1] Create New Customer Account                          ");
            System.out.println("  [2] Shop & Order Products                      ");
            System.out.println("  [3] Browse Product Catalog                      ");
            System.out.println("  [4] Exit Shopping Portal                        ");
            System.out.println("........................................................");
            System.out.print("Select your action: ");

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
                    System.out.println(" Thank you for choosing SYOS! Come back soon! ");
                    return;
                default:
                    System.out.println("  Invalid selection. Please choose from the menu above.");
            }

            System.out.println("\n Press Enter to continue...");
            scanner.nextLine();
        }
    }

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

    private void displayShoppingBanner() {
        System.out.println("...................................................................");
        System.out.println("               SYNEX ONLINE SHOPPING EXPERIENCE                 ");
        System.out.println("                                                               ");
        System.out.println("              Fast • Secure • Convenient Shopping               ");
        System.out.println("                                                               ");
        System.out.println("...................................................................");
        System.out.println();
    }

    private void registerCustomer() throws SQLException, ClassNotFoundException {
        System.out.println("\n...............................................");
        System.out.println("           ACCOUNT CREATION WIZARD           ");
        System.out.println("   Create your personal shopping profile     ");
        System.out.println("...............................................");
        System.out.println();

        System.out.print(" Full Name : ");
        String name = scanner.nextLine();

        System.out.print(" Mobile Number : ");
        String phone = scanner.nextLine();

        System.out.print(" Email Address : ");
        String email = scanner.nextLine();

        System.out.print(" Delivery Address : ");
        String address = scanner.nextLine();

        Customer customer = new Customer(name, phone);

        try {
            if (onlineController.registerOnlineCustomer(customer, email, address)) {
                System.out.println("\n Account Created Successfully!");
                System.out.println(" Welcome to SYOS, " + name + "!");
                System.out.println(" Your Customer ID: " + customer.getId());
                System.out.println(" You can now shop using your mobile number: " + phone);
            } else {
                System.out.println(" Account creation unsuccessful. Please retry.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("  Account Creation Issues Detected:");
            System.out.println(" " + e.getMessage());
            System.out.println(" Please verify your information and try again.");
        }
    }

    private void placeOrder() throws SQLException, ClassNotFoundException {
        System.out.println("\n...............................................");
        System.out.println("           ORDER PLACEMENT CENTER          ");
        System.out.println("      Shop smart, shop with confidence       ");
        System.out.println("...............................................");
        System.out.println();

        // Get customer
        System.out.print(" Enter your registered mobile number : ");
        String phone = scanner.nextLine();

        Customer customer = customerController.get_OnlineCustomer_from_contactNumber(phone);
        if (customer == null) {
            System.out.println(" Account not found with this number.");
            System.out.println(" Please create an account first from the main menu.");
            return;
        }

        System.out.println(" Welcome back, " + customer.getName() + "!");

        showAvailableItems();

        List<BillItem> orderItems = new ArrayList<>();
        boolean addMoreItems = true;

        while (addMoreItems) {
            System.out.print("\n  Item Code (Enter to complete order) : ");
            String itemCode = scanner.nextLine();

            if (itemCode.isEmpty()) {
                addMoreItems = false;
                break;
            }

            Item item = itemController.getItemFromCode(itemCode);
            if (item == null) {
                System.out.println(" Item code not recognized! Please check and retry.");
                continue;
            }

            System.out.print(" Quantity needed : ");
            int quantity = scanner.nextInt();
            scanner.nextLine();

            if (onlineController.isItemAvailableOnline(item.getId(), quantity)) {
                BillItem billItem = new BillItem(item, quantity);
                orderItems.add(billItem);
                System.out.println(" Added to cart: " + item.getName() + " × " + quantity +
                        " = Rs." + billItem.getTotalPrice());
            } else {
                System.out.println("  Insufficient inventory for this Item!");
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
        System.out.println("\n...............................................");
        System.out.println("             AVAILABLE Items               ");
        System.out.println("...............................................");
        List<Item> items = onlineController.getWebsiteInventory();

        if (items.isEmpty()) {
            System.out.println("No items available online at the moment.");
            return;
        }

        System.out.printf("%-10s %-20s %-10s%n", "Code", "Name", "Price");
        System.out.println("------------------------------------------------------------");

        for (Item item : items) {
            System.out.printf("%-10s %-20s Rs.%-9.2f%n",
                    item.getCode(), item.getName(), item.getPrice());
        }
    }
}
