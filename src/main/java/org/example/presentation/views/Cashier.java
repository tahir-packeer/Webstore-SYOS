package org.example.presentation.views;

import org.example.presentation.controllers.BillController;
import org.example.presentation.controllers.CustomerController;
import org.example.presentation.controllers.ItemController;
import org.example.persistence.models.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Cashier {

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

    private void displayCashierBanner(User user) {
        System.out.println("...................................................");
        System.out.println("        SYOS Point of Sale System     ");
        System.out.println("        Cashier: " + user.getName());
        System.out.println("...................................................");
        System.out.println();
    }

    public void cashierInterface(User user) throws SQLException, ClassNotFoundException {
        List<BillItem> itemsOfBill = new ArrayList<>();
        double billAmount = 0;
        Customer Customer = null;

        clearScreen();
        displayCashierBanner(user);

        boolean addMoreItems = true;

        while (addMoreItems) {
            System.out.print("Enter Item Code (press Enter when done): ");
            String itemCode = scanner.nextLine();

            if (itemCode.equals("")) {
                addMoreItems = false;
                break;
            }

            Item selectedItem = new ItemController().getItemFromCode(itemCode);

            if (selectedItem != null) {
                System.out.println("Item: " + selectedItem.getName());
                System.out.println("Price: Rs." + selectedItem.getPrice());

                System.out.print("Enter Quantity: ");
                int itemQuantity = scanner.nextInt();
                scanner.nextLine();

                BillItem billItem = new BillItem(selectedItem, itemQuantity);

                itemsOfBill.add(billItem);
                billAmount += billItem.getTotalPrice();

                System.out.println("\n........................................................................");
                System.out.println("                            TRANSACTION SUMMARY                         ");
                System.out.println("........................................................................");
                System.out.printf("%-10s | %-20s | %-10s | %-8s | %-12s%n", "Code", "Product", "Unit Price", "Qty",
                        "Line Total");
                System.out.println("-----------|----------------------|------------|----------|------------");

                for (BillItem bi : itemsOfBill) {
                    Item item = bi.getItem();
                    int quantity = bi.getQuantity();
                    double totalPrice = bi.getTotalPrice();

                    System.out.printf("%-10s | %-20s | Rs.%-7.2f | %-8d | Rs.%-9.2f%n",
                            item.getCode(),
                            item.getName(),
                            item.getPrice(),
                            quantity,
                            totalPrice);
                }

                System.out.println("........................................................................");
                System.out.printf("                                     RUNNING TOTAL: Rs.%-18.2f%n", billAmount);
                System.out.println("........................................................................");
            } else {
                System.out.println("Product code not recognized. Please try again.");
            }
        }

        if (itemsOfBill.isEmpty()) {
            System.out.println("No products scanned. Transaction cancelled.");
            return;
        }

        System.out.println("\n...................................................");
        System.out.println("              CUSTOMER INFORMATION              ");
        System.out.println(".....................................................");

        System.out.print("Customer Mobile Number : ");
        String customerPhone = scanner.nextLine();

        if (customerPhone.length() > 9) {
            Customer = new CustomerController().get_Customer_from_contactNumber(customerPhone);
            if (Customer == null) {
                System.out.println("New customer detected. Please register:");
                System.out.print("Customer Full Name: ");
                String customerName = scanner.nextLine();

                Customer = new Customer(customerName, customerPhone);
                new CustomerController().add_Customer(Customer);
                System.out.println("Customer registered successfully!");
            } else {
                System.out.println("Customer verified: " + Customer.getName());
            }
        } else {
            System.out.println("Invalid mobile number format. Please enter a valid number.");
            return;
        }

        String InvoiceNumber = new BillController().getInvoiceNumber();

        System.out.println("\n...................................................");
        System.out.println("              PAYMENT PROCESSING              ");
        System.out.println(".....................................................");

        System.out.printf("Transaction Total: Rs.%.2f%n", billAmount);
        System.out.print("Discount Amount : Rs.");
        double discount = scanner.nextDouble();

        billAmount -= discount;
        System.out.printf("Amount Due: Rs.%.2f%n", billAmount);

        double amountTendered;
        do {
            System.out.print("Cash Received : Rs.");
            amountTendered = scanner.nextDouble();

            if (amountTendered < billAmount) {
                System.out.println("Insufficient payment! Please collect Rs." +
                        String.format("%.2f", (billAmount - amountTendered)) + " more.");
            }

        } while (amountTendered < billAmount);

        double balance = amountTendered - billAmount;
        System.out.printf("Change to Return: Rs.%.2f%n", balance);

        Bill bill = new Bill(
                Customer,
                InvoiceNumber,
                billAmount,
                discount,
                amountTendered,
                balance);

        Bill finalBill = new BillController().Add_Bill(bill);

        new BillController().add_Bill_items(itemsOfBill, finalBill);

        System.out.println("\nTransaction completed successfully!");
        System.out.println("Bill ID: " + finalBill.getId());
        System.out.println("Invoice Number: " + finalBill.getInvoiceNumber());

        String fileName = "Receipt_" + finalBill.getInvoiceNumber() + ".txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(".............................................................\n");
            writer.write("                     SYNEX POS                       \n");
            writer.write("                SYOS Management System           \n");
            writer.write("                                                     \n");
            writer.write("Address: 123 Commerce Street, Business District      \n");
            writer.write("Contact: +94-11-234-5678 | SYOS@store.com           \n");
            writer.write(".............................................................\n");
            writer.write("\n");
            writer.write("Receipt #: " + finalBill.getInvoiceNumber() + "\n");
            writer.write("Transaction Date: " + finalBill.getBillDate() + "\n");

            if (finalBill.getCustomer() != null) {
                writer.write("Customer: " + finalBill.getCustomer().getName() + "\n");
                writer.write("Mobile: " + finalBill.getCustomer().getcontactNumber() + "\n");
            } else {
                writer.write("Customer: Walk-in Purchase\n");
            }

            writer.write("Processed: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    + "\n");
            writer.write("Operator: " + user.getName() + " (Terminal: CASH-001)\n");
            writer.write("\n");
            writer.write("------------------------------------------------------------------\n");
            writer.write("  #       Item Name         Price    Qty   Total Amount  \n");
            writer.write("------------------------------------------------------------------\n");

            int itemNumber = 1;
            for (BillItem billItem : itemsOfBill) {
                Item item = billItem.getItem();
                writer.write(String.format(" %-4d     %-18s     %8.2f   %3d     %10.2f %n",
                        itemNumber++,
                        item.getName(),
                        item.getPrice(),
                        billItem.getQuantity(),
                        billItem.getTotalPrice()));
            }

            writer.write("------------------------------------------------------------------\n");
            writer.write("\n");
            writer.write("Subtotal:        Rs."
                    + String.format("%10.2f", finalBill.getFullPrice() + finalBill.getDiscount()) + "\n");
            writer.write("Discount:        Rs." + String.format("%10.2f", finalBill.getDiscount()) + "\n");
            writer.write("Total Amount:    Rs." + String.format("%10.2f", finalBill.getFullPrice()) + "\n");
            writer.write("Cash Paid:       Rs." + String.format("%10.2f", finalBill.getCashTendered()) + "\n");
            writer.write("Change:          Rs." + String.format("%10.2f", finalBill.getChangeAmount()) + "\n");
            writer.write("\n");
            writer.write(".............................................................\n");
            writer.write("Thank you for shopping at SYNEX! \n");
            writer.write(".............................................................\n");

            System.out.println("Receipt generated successfully: " + fileName);
            System.out.println("Saved to: " + new File(fileName).getAbsolutePath());

        } catch (IOException e) {
            System.out.println("Receipt generation error: " + e.getMessage());
        }

    }

}
