package org.example.presentation.views;

import org.example.presentation.controllers.ReportController;
import org.example.persistence.models.User;

import java.util.Scanner;

public class Admin {

    Scanner scanner = new Scanner(System.in);

    public void adminInterface(User user) {
        System.out.println("\n...................................................");
        System.out.println("            SYOS ADMIN CONTROL PANEL            ");
        System.out.println("...................................................");

        while (true) {
            System.out.println("\n...................................................");
            System.out.println("                 ADMIN MENU                     ");
            System.out.println("...................................................");
            System.out.println("  [1] Daily Sales Report");
            System.out.println("  [2] Items Need Shelving");
            System.out.println("  [3] Reorder Level Report");
            System.out.println("  [4] Stock Report");
            System.out.println("  [5] Stock Batch Report");
            System.out.println("  [6] Bill Transaction Report");
            System.out.println("  [7] Combined Reports (by Transaction Type)");
            System.out.println("  [8] Combined Reports (by Store Type)");
            System.out.println("  [9] Exit");
            System.out.println("...................................................");
            System.out.print("Enter your choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    generateDailySalesReport(user);
                    break;
                case 2:
                    generateItemsNeedShelving(user);
                    break;
                case 3:
                    generateReorderLevelReport(user);
                    break;
                case 4:
                    generateReorderStockReport(user);
                    break;
                case 5:
                    generateStockBatchReport(user);
                    break;
                case 6:
                    generateBillTransactionReport(user);
                    break;
                case 7:
                    generateReportsByTransactionType(user);
                    break;
                case 8:
                    generateReportsByStoreType(user);
                    break;
                case 9:
                    System.out.println(" Exiting Admin Panel...");
                    return;
                default:
                    System.out.println(" Invalid choice. Please try again.");
            }
        }
    }

    private void generateDailySalesReport(User user) {
        System.out.print("Enter date (YYYY-MM-DD): ");
        String date = scanner.nextLine();

        ReportController reportController = new ReportController();
        try {
            reportController.generate_sales_report(date);
        } catch (Exception e) {
            System.out.println("Error generating daily sales report: " + e.getMessage());
        }

        adminInterface(user);
    }

    private void generateItemsNeedShelving(User user) {
        ReportController reportController = new ReportController();
        try {
            reportController.generate_items_need_shelving_report();
        } catch (Exception e) {
            System.out.println("Error generating items need shelving report: " + e.getMessage());
        }

        adminInterface(user);
    }

    private void generateReorderLevelReport(User user) {
        ReportController reportController = new ReportController();
        try {
            reportController.generate_reorder_level_report();
        } catch (Exception e) {
            System.out.println("Error generating reorder level report: " + e.getMessage());
        }

        adminInterface(user);
    }

    public void generateReorderStockReport(User user) {
        ReportController reportController = new ReportController();
        try {
            reportController.generate_reorder_stock_report();
        } catch (Exception e) {
            System.out.println("Error generating reorder stock report: " + e.getMessage());
        }

        adminInterface(user);
    }

    private void generateStockBatchReport(User user) {
        ReportController reportController = new ReportController();
        try {
            reportController.generate_stock_batch_report();
        } catch (Exception e) {
            System.out.println("Error generating stock batch report: " + e.getMessage());
        }

        adminInterface(user);
    }

    private void generateBillTransactionReport(User user) {
        ReportController reportController = new ReportController();
        System.out.print("Enter the start date for the bill transaction report (YYYY-MM-DD): ");
        String startDate = scanner.nextLine();

        System.out.print("Enter the end date for the bill transaction report (YYYY-MM-DD): ");
        String endDate = scanner.nextLine();
        try {
            reportController.generate_bill_transaction_report(startDate, endDate);
        } catch (Exception e) {
            System.out.println("Error generating bill transaction report: " + e.getMessage());
        }

        adminInterface(user);
    }

    private void generateReportsByTransactionType(User user) {
        System.out.println("\n...................................................");
        System.out.println("        SELECT TRANSACTION TYPE                  ");
        System.out.println("...................................................");
        System.out.println("  [1] Counter Sales");
        System.out.println("  [2] Online Sales");
        System.out.println("...................................................");
        System.out.print("Enter choice: ");

        int choice = scanner.nextInt();
        scanner.nextLine();

        String transactionType = (choice == 1) ? "COUNTER" : "ONLINE";

        System.out.print("Enter date (YYYY-MM-DD): ");
        String date = scanner.nextLine();

        ReportController reportController = new ReportController();
        try {
            System.out.println("\n...................................................");
            System.out.println("      Sales Report by Transaction Type          ");
            System.out.println("...................................................");
            reportController.generate_sales_report(date, transactionType, null);

            System.out.println("\n...................................................");
            System.out.println("   Bill Transaction Report by Transaction Type  ");
            System.out.println("...................................................");
            reportController.generate_bill_transaction_report(date, date, transactionType, null);
        } catch (Exception e) {
            System.out.println("Error generating reports: " + e.getMessage());
        }

        adminInterface(user);
    }

    private void generateReportsByStoreType(User user) {
        System.out.println("\n...................................................");
        System.out.println("            SELECT STORE TYPE                    ");
        System.out.println("...................................................");
        System.out.println("  [1] Physical Store");
        System.out.println("  [2] Website");
        System.out.println("...................................................");
        System.out.print("Enter choice: ");

        int choice = scanner.nextInt();
        scanner.nextLine();

        String storeType = (choice == 1) ? "STORE" : "WEBSITE";

        System.out.print("Enter date (YYYY-MM-DD): ");
        String date = scanner.nextLine();

        ReportController reportController = new ReportController();
        try {
            System.out.println("\n...................................................");
            System.out.println("         Sales Report by Store Type             ");
            System.out.println("...................................................");
            reportController.generate_sales_report(date, null, storeType);

            System.out.println("\n...................................................");
            System.out.println("      Bill Transaction Report by Store Type     ");
            System.out.println("...................................................");
            reportController.generate_bill_transaction_report(date, date, null, storeType);
        } catch (Exception e) {
            System.out.println("Error generating reports: " + e.getMessage());
        }

        adminInterface(user);
    }
}
