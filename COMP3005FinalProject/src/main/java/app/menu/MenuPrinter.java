package app.menu;

import java.util.Scanner;

public class MenuPrinter {
    private static final Scanner sc = new Scanner(System.in);

    // Displays the main login menu and returns the user's choice
    public static int showLoginMenu() {
        System.out.println("=== FitnessClub CLI ===");
        System.out.println("1) Member Login");
        System.out.println("2) Trainer Login");
        System.out.println("3) Admin Login");
        System.out.println("4) Register as a New Member");
        System.out.println("0) Exit");
        System.out.print("Choice: ");
        return readInt();
    }


    // Prompts the user to enter their ID based on their role and returns the entered ID
    public static int askForId(String roleName) {
        System.out.printf("Enter %s ID: ", roleName);
        return readInt();
    }

    // Displays the member menu and returns the user's choice
    public static int showMemberMenu() {
        System.out.println("\n=== Member Menu ===");
        System.out.println("1) Dashboard");
        System.out.println("2) Update Personal Details");
        System.out.println("3) Update Fitness Goals");
        System.out.println("4) Add Health Metric");
        System.out.println("5) Register for Class");
        System.out.println("6) View Registered Classes");
        System.out.println("7) View Health History (last 5 entries)");
        System.out.println("0) Logout");
        System.out.print("Choice: ");
        return readInt();
    }


    // Displays the trainer menu and returns the user's choice
    public static int showTrainerMenu() {
        System.out.println("\n=== Trainer Menu ===");
        System.out.println("1) Set Availability");
        System.out.println("2) View My Schedule");
        System.out.println("0) Logout");
        System.out.print("Choice: ");
        return readInt();
    }

    // Displays the admin menu and returns the user's choice
    public static int showAdminMenu() {
        System.out.println("\n=== Admin Menu ===");
        System.out.println("1) Create Class");
        System.out.println("2) Update Class");
        System.out.println("3) View All Classes");
        System.out.println("4) View MemberClassSchedule (VIEW)");
        System.out.println("5) Cancel Class / Session");   // NEW
        System.out.println("0) Logout");
        System.out.print("Choice: ");
        return readInt();
    }


    // Helper method to read an integer input from the user
    private static int readInt() {
        // Validate input
        while (!sc.hasNextInt()) {
            sc.next();
            System.out.print("Please enter a number: ");
        }
        return sc.nextInt();
    }
}
