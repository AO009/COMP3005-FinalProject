package app;

import app.db.DatabaseManager;
import app.menu.MenuPrinter;
import app.auth.AuthService;
import app.actions.MemberActions;
import app.actions.TrainerActions;
import app.actions.AdminActions;
import app.actions.RegistrationActions;


public class Main {
    public static void main(String[] args) {

        // Establish a connection to the database
        DatabaseManager.connect();

        // Main application loop
        while (true) {
            int choice = MenuPrinter.showLoginMenu();

            // Handle user choice
            switch (choice) {
                case 1 -> handleMemberLogin();
                case 2 -> handleTrainerLogin();
                case 3 -> handleAdminLogin();
                case 4 -> RegistrationActions.registerNewMember();  // NEW
                case 0 -> {
                    System.out.println("Goodbye!");
                    DatabaseManager.close();
                    return;
                }
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
    }




    // Handle member login process
    private static void handleMemberLogin() {

        // Prompt for member ID and validate
        int id = MenuPrinter.askForId("Member");
        if (!AuthService.isValidMember(id)) {
            System.out.println("Invalid Member ID.\n");
            return;
        }
        System.out.println("Login successful.\n");
        // Launch member menu
        MemberActions.runMenu(id);
    }

    // Handle trainer login process
    private static void handleTrainerLogin() {

        // Prompt for trainer ID and validate
        int id = MenuPrinter.askForId("Trainer");
        if (!AuthService.isValidTrainer(id)) {
            System.out.println("Invalid Trainer ID.\n");
            return;
        }
        System.out.println("Login successful.\n");

        // Launch trainer menu
        TrainerActions.runMenu(id);
    }

    private static void handleAdminLogin() {

        // Prompt for admin ID and validate
        int id = MenuPrinter.askForId("Admin");
        if (!AuthService.isValidAdmin(id)) {
            System.out.println("Invalid Admin ID.\n");
            return;
        }
        System.out.println("Login successful.\n");
        // Launch admin menu
        AdminActions.runMenu(id);
    }
}
