package org.example;

import org.example.Controller.Authentication;
import org.example.View.OnlineStore;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws SQLException, ClassNotFoundException, ParseException {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== Welcome to Synex Outlet Store (SYOS) ===");
        System.out.println("Select Interface:");
        System.out.println("1. Staff Login (Cashier/Manager/Admin)");
        System.out.println("2. Online Store (Customer Interface)");
        System.out.print("Enter choice: ");
        
        int choice = scanner.nextInt();
        scanner.nextLine(); 
        
        switch (choice) {
            case 1:
                Authentication.startLoginProcess();
                break;
            case 2:
                OnlineStore onlineStore = new OnlineStore();
                onlineStore.startOnlineStore();
                break;
            default:
                System.out.println("Invalid choice. Starting staff login...");
                Authentication.startLoginProcess();
        }
        
        scanner.close();
    }
}