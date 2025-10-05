package org.example.View;

import org.example.Controller.BillController;
import org.example.Controller.CustomerController;
import org.example.Controller.ItemController;
import org.example.Model.*;

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

    public void cashierInterface(User user) throws SQLException, ClassNotFoundException {
        List<BillItem> itemsOfBill = new ArrayList<>();
        double billAmount = 0;
        Customer Customer = null;

        boolean addMoreItems = true;

        while (addMoreItems) {
            System.out.print("Enter Item Code: ");
            String itemCode = scanner.nextLine();

            if (itemCode.equals("")) {
                addMoreItems = false;
                break;
            }

            Item selectedItem = new ItemController().getItemFromCode(itemCode);

            if (selectedItem != null) {
                System.out.println("Item Name: " + selectedItem.getName());

                System.out.println("Item Price: Rs." + selectedItem.getPrice());

                System.out.print("Enter Item Quantity: ");
                int itemQuantity = scanner.nextInt();
                scanner.nextLine();

                BillItem billItem = new BillItem(selectedItem, itemQuantity);

                itemsOfBill.add(billItem);
                billAmount += billItem.getTotalPrice();

                System.out.printf("%-10s %-20s %-10s %-10s %-15s%n", "Item Code", "Item Name", "Price", "Quantity", "Total Price");
                System.out.println("---------------------------------------------------------------");

                for (BillItem bi : itemsOfBill) {
                    Item item = bi.getItem();
                    int quantity = bi.getQuantity();
                    double totalPrice = bi.getTotalPrice();

                    System.out.printf("%-10s %-20s Rs.%-10.2f %-10d Rs.%-15.2f%n",
                            item.getCode(),
                            item.getName(),
                            item.getPrice(),
                            quantity,
                            totalPrice);
                }

                System.out.println("---------------------------------------------------------------");
                System.out.printf("Total Bill Amount: Rs.%.2f%n", billAmount);
            }
        }

        if (itemsOfBill.isEmpty()) {
            System.out.println("No items added to the bill.");
            return;
        }

        System.out.print("Enter Customer Phone Number: ");
        String customerPhone = scanner.nextLine();

        if (customerPhone.length() > 9) {
            Customer customer = new CustomerController().get_Customer_from_contactNumber(customerPhone);
            if (customer == null) {
                System.out.println("Customer not found. Please add the customer");
                System.out.print("Enter Customer Name: ");
                String customerName = scanner.nextLine();

                Customer newCustomer = new Customer(customerName, customerPhone);

                new CustomerController().add_Customer(newCustomer);
            } else {
                System.out.println("Customer found: " + customer.getName());
            }
        } else {
            System.out.println("Invalid phone number. Please enter a valid phone number.");
            return;
        }

        String InvoiceNumber = new BillController().getInvoiceNumber();

        System.out.println("Your Total is: LKR." + billAmount);
        System.out.println("Discount (If any):");
        double discount = scanner.nextDouble();
        double amountTendered;
        do {
            System.out.println("Amount Tendered:");
            amountTendered = scanner.nextDouble();

            billAmount -= discount;

            if (amountTendered < billAmount) {
                System.out.println("Amount tendered is less than the bill amount. Please enter a valid amount.");
            }



        } while (amountTendered < billAmount);

        double balance = amountTendered - billAmount;
        System.out.printf("Balance: LKR %.2f%n", balance);


        Bill bill = new Bill(
                Customer,
                InvoiceNumber,
                billAmount,
                discount,
                amountTendered,
                balance
        );

        Bill finalBill = new BillController().Add_Bill(bill);

        new BillController().add_Bill_items(itemsOfBill, finalBill);

        System.out.println("Bill added successfully with ID: " + finalBill.getId());

        String fileName = "Bill_" + finalBill.getInvoiceNumber() + ".txt";


        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write("========== Synex Outlet Store ==========\n");
            writer.write("Invoice No: " + finalBill.getInvoiceNumber() + "\n");
            writer.write("Bill Date: " + finalBill.getBillDate() + "\n");

            if (finalBill.getCustomer() != null) {
                writer.write("Customer Name: " + finalBill.getCustomer().getName() + "\n");
                writer.write("Customer Phone: " + finalBill.getCustomer().getcontactNumber() + "\n");
            } else {
                writer.write("Customer: Walk-in\n");
            }

            writer.write("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
            writer.write("Cashier: " + user.getName() + "\n");
            writer.write("----------------------------------------\n");
            writer.write(String.format("%-5s %-20s %-10s %-10s %-15s%n", "#", "Item Name", "Price", "Qty", "Total Price"));
            writer.write("----------------------------------------\n");

            int itemNumber = 1;
            for (BillItem billItem : itemsOfBill) {
                Item item = billItem.getItem();
                writer.write(String.format("%-5d %-20s %-10.2f %-10d %-15.2f%n",
                        itemNumber++,
                        item.getName(),
                        item.getPrice(),
                        billItem.getQuantity(),
                        billItem.getTotalPrice()));
            }

            writer.write("----------------------------------------\n");
            writer.write(String.format("Total Bill Amount: %.2f%n", finalBill.getFullPrice()));
            writer.write(String.format("Discount: %.2f%n", finalBill.getDiscount()));
            writer.write(String.format("Cash Tendered: %.2f%n", finalBill.getCashTendered()));
            writer.write(String.format("Change: %.2f%n", finalBill.getChangeAmount()));
            writer.write("========================================\n");

            System.out.println("Bill exported successfully to file: " + fileName);
            System.out.println("Saving invoice to: " + new File(fileName).getAbsolutePath());

        } catch (IOException e) {
            System.out.println("Error writing bill to file: " + e.getMessage());
        }


    }

}

