package org.example;

import org.example.presentation.controllers.Authentication;
import org.example.presentation.views.OnlineStore;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws SQLException, ClassNotFoundException, ParseException {
        Scanner scanner = new Scanner(System.in);

        // Clear screen
        clearScreen();
        displayWelcomeBanner();

        System.out.println("┌─────────────────────────────────────────────────┐");
        System.out.println("│                  ACCESS PORTAL                  │");
        System.out.println("├─────────────────────────────────────────────────┤");
        System.out.println("│  [1] Employee Portal                            │");
        System.out.println("│  [2] Customer Portal                            │");
        System.out.println("└─────────────────────────────────────────────────┘");
        System.out.print("Select your access type:  ");

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
                System.out.println("Invalid selection! Redirecting to Employee Portal...");
                Authentication.startLoginProcess();
        }

        scanner.close();
    }

    private static void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[2J\033[H");
            }
        } catch (Exception e) {
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }

    private static void displayWelcomeBanner() {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║              ███████╗██╗   ██╗ ██████╗ ███████╗              ║");
        System.out.println("║              ██╔════╝╚██╗ ██╔╝██╔═══██╗██╔════╝              ║");
        System.out.println("║              ███████╗ ╚████╔╝ ██║   ██║███████╗              ║");
        System.out.println("║              ╚════██║  ╚██╔╝  ██║   ██║╚════██║              ║");
        System.out.println("║              ███████║   ██║   ╚██████╔╝███████║              ║");
        System.out.println("║              ╚══════╝   ╚═╝    ╚═════╝ ╚══════╝              ║");
        System.out.println("║                                                              ║");
        System.out.println("║                   SYNEX OUTLET STORE SYSTEM                  ║");
        System.out.println("║                                                              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
    }
}
