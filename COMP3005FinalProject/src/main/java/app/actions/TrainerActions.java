package app.actions;

import app.db.DatabaseManager;
import app.menu.MenuPrinter;

import java.sql.*;
import java.util.Scanner;

public class TrainerActions {

    private static final Scanner sc = new Scanner(System.in);

    public static void runMenu(int trainerId) {
        while (true) {
            int choice = MenuPrinter.showTrainerMenu();
            switch (choice) {
                case 1 -> setAvailability(trainerId); // Set Availability: Define time windows when available for sessions or classes. Prevent overlap.
                case 2 -> viewSchedule(trainerId); // Schedule View: See assigned PT sessions and classes.
                case 0 -> {
                    System.out.println("Logging out...\n");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    // Trainer Operation: Set Availability
    private static void setAvailability(int trainerId) {

        // Establish a connection
        Connection conn = DatabaseManager.getConnection();

        System.out.println("\n=== Set Availability ===");
        System.out.println("Existing availability for you:");

        // List existing availability
        String listSql = """
                SELECT date, start_hour, end_hour
                FROM IndividualAvailability
                 WHERE trainer_id = ?
                ORDER BY date, start_hour
                """;

        // inject trainerId and execute
        try (PreparedStatement ps = conn.prepareStatement(listSql)) {
            ps.setInt(1, trainerId);
            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.printf("  %s | %s–%s%n", rs.getDate("date"), rs.getTime("start_hour"), rs.getTime("end_hour"));
                }
                if (!any) {
                    System.out.println("  (No availability defined yet)");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error listing existing availability:");
            e.printStackTrace();
        }

        // Prompt for new availability
        System.out.println("\nEnter new availability window.");
        System.out.print("Date (YYYY-MM-DD): ");
        String dateStr = sc.nextLine().trim();

        System.out.print("Start time (HH:MM, 24h): ");
        String startStr = sc.nextLine().trim();

        System.out.print("End time (HH:MM, 24h): ");
        String endStr = sc.nextLine().trim();

        // Convert to SQL types
        Date date;
        Time startTime;
        Time endTime;

        try {
            date = Date.valueOf(dateStr);

            // adding seconds to match format
            startTime = Time.valueOf(startStr + ":00");
            endTime   = Time.valueOf(endStr + ":00");
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid date or time format.");
            return;
        }

        // Insert new availability
        String insertSql = """
                INSERT INTO IndividualAvailability (trainer_id, start_hour, end_hour, date)
                VALUES (?, ?, ?, ?)
                """;

        // inject data and execute
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setInt(1, trainerId);
            ps.setTime(2, startTime);
            ps.setTime(3, endTime);
            ps.setDate(4, date);

            ps.executeUpdate();
            System.out.println("Availability added successfully.");

        } catch (SQLException e) {
            System.out.println("Error adding availability:");
            System.out.println("DB message: " + e.getMessage());
        }
    }

    // Trainer Operation: View Schedule
    private static void viewSchedule(int trainerId) {

        System.out.println("\n=== My Schedule (Classes & Sessions) ===");

        // Query to get classes assigned to the trainer
        String sql = """
                SELECT fc.id, fc.date, fc.start_hour, fc.end_hour,r.location AS room_location, COUNT(rg.member_id) AS registered_count
                FROM FitnessClass fc
                JOIN Room r ON fc.room_id = r.id
                LEFT JOIN Registers rg ON rg.fitness_class_id = fc.id
                 WHERE fc.trainer_id = ?
                GROUP BY fc.id, fc.date, fc.start_hour, fc.end_hour, r.location
                ORDER BY fc.date, fc.start_hour
                """;

        // Establish a connection
        Connection conn = DatabaseManager.getConnection();

        // inject trainerId and execute
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, trainerId);
            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.printf(
                            "Class ID: %d | %s | %s–%s | Room: %s | Registered: %d%n",  rs.getInt("id"), rs.getDate("date"),
                            rs.getTime("start_hour"), rs.getTime("end_hour"), rs.getString("room_location"),
                            rs.getInt("registered_count")
                    );
                }
                if (!any) {
                    System.out.println("You have no classes assigned.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching schedule:");
            e.printStackTrace();
        }
    }

    // Helper method to read an integer from input
    private static int readInt() {

        // Validate input
        while (!sc.hasNextInt()) {
            sc.next();
            System.out.print("Please enter a number: ");
        }

        // Read integer value
        int value = sc.nextInt();
        sc.nextLine(); // consume newline
        return value;
    }
}
