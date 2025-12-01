package app.actions;

import app.db.DatabaseManager;
import app.menu.MenuPrinter;

import java.sql.*;
import java.util.Scanner;

public class MemberActions {

    private static final Scanner sc = new Scanner(System.in);

    public static void runMenu(int memberId) {
        while (true) {
            int choice = MenuPrinter.showMemberMenu();
            switch (choice) {
                case 1 -> showDashboard(memberId); // correlates to the Member operation "Dashboard"
                case 2 -> updatePersonalDetails(memberId); // Correlates to the Member operation "Profile Management"
                case 3 -> updateFitnessGoals(memberId); // Correlates to the Member operation "Profile Management"
                case 4 -> addHealthMetric(memberId); // Correlates to the Member operation "Health History"
                case 5 -> registerForClass(memberId); // Correlates to the Member operation "Group Class Registration"
                case 6 -> viewRegisteredClasses(memberId); // Nice to have helper
                case 7 -> viewHealthHistory(memberId); // nice to have helper
                case 0 -> {
                    System.out.println("Logging out...\n");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }


    // Dashboard: Show latest health stats, active goals, past class count, upcoming sessions.
    private static void showDashboard(int memberId) {
        Connection conn = DatabaseManager.getConnection();

        System.out.println("\n=== Member Dashboard ===");

        // Latest health metric
        String latestMetricSql = """
                SELECT timestamp, height, weight, vo2_max, body_fat_percentage
                FROM HealthMetric
                WHERE member_id = ?
                 ORDER BY timestamp DESC
                LIMIT 1
                """;

        // Latest goals
        String latestGoalSql = """
                SELECT timestamp, weight_goal, vo2_max_goal, body_fat_percentage_goal
                FROM FitnessGoals
                WHERE member_id = ?
                ORDER BY timestamp DESC
                LIMIT 1
                """;

        // Past class count
        String pastCountSql = """
                SELECT COUNT(*) AS past_count
                FROM Registers r
                JOIN FitnessClass fc ON r.fitness_class_id = fc.id
                WHERE r.member_id = ? AND fc.date < CURRENT_DATE
                """;

        // Upcoming sessions
        String upcomingSql = """
                SELECT fitness_class_id, date, start_hour, end_hour, room_location, trainer_fname, trainer_lname
                FROM MemberClassSchedule
                WHERE member_id = ? AND date >= CURRENT_DATE
                ORDER BY date, start_hour
                LIMIT 5
                """;

        try {
            // Latest metric
            try (PreparedStatement ps = conn.prepareStatement(latestMetricSql)) {
                ps.setInt(1, memberId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("Latest Health Metric:");
                        System.out.printf("  At: %s | Height: %.1f cm | Weight: %.1f kg | VO2: %.1f | Body fat: %.1f%%%n",  rs.getTimestamp("timestamp"),
                                rs.getDouble("height"),  rs.getDouble("weight"), rs.getDouble("vo2_max"),
                                rs.getDouble("body_fat_percentage"));
                    } else {
                        System.out.println("Latest Health Metric: none recorded yet.");
                    }
                }
            }

            // Latest goal
            try (PreparedStatement ps = conn.prepareStatement(latestGoalSql)) {
                ps.setInt(1, memberId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("Active Fitness Goals:");
                        System.out.printf("  Weight: %.1f kg | VO2: %.1f | Body fat: %.1f%%%n", rs.getDouble("weight_goal"),
                                rs.getDouble("vo2_max_goal"),  rs.getDouble("body_fat_percentage_goal"));
                    } else {
                        System.out.println("Active Fitness Goals: none set yet.");
                    }
                }
            }

            // Past class count
            try (PreparedStatement ps = conn.prepareStatement(pastCountSql)) {
                ps.setInt(1, memberId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("Past classes attended: " + rs.getInt("past_count"));
                    }
                }
            }

            // Upcoming sessions
            System.out.println("Upcoming Classes:");
            try (PreparedStatement ps = conn.prepareStatement(upcomingSql)) {
                ps.setInt(1, memberId);
                try (ResultSet rs = ps.executeQuery()) {
                    boolean any = false;
                    while (rs.next()) {
                        any = true;
                        System.out.printf("  Class %d | %s | %s–%s | Room: %s | Trainer: %s %s%n", rs.getInt("fitness_class_id"),
                                rs.getDate("date"),  rs.getTime("start_hour"), rs.getTime("end_hour"),
                                rs.getString("room_location"), rs.getString("trainer_fname"), rs.getString("trainer_lname"));
                    }
                    if (!any) {
                        System.out.println("  (No upcoming sessions)");
                    }
                }
            }

        } catch (SQLException e) {
            System.out.println("Error building dashboard:");
            e.printStackTrace();
        }
    }


    // "Profile Management": Update personal details
    private static void updatePersonalDetails(int memberId) {

        // Establish a connection
        Connection conn = DatabaseManager.getConnection();

        // Prompt for new details
        System.out.println("\n=== Update Personal Details ===");
        System.out.print("New first name (leave blank to keep current): ");
        String fname = sc.nextLine().trim();
        System.out.print("New last name (leave blank to keep current): ");
        String lname = sc.nextLine().trim();
        System.out.print("New email (leave blank to keep current): ");
        String email = sc.nextLine().trim();
        System.out.print("New phone (leave blank to keep current): ");
        String phone = sc.nextLine().trim();

        // Fetch current details if fields are left blank
        String selectSql = "SELECT fname, lname, email, phone_number FROM Member WHERE id = ?";

        // Update statement
        String updateSql = """
                UPDATE Member
                SET fname = ?,  lname = ?, email = ?, phone_number = ?
                WHERE id = ?
                """;

        // Execute select to get current details
        try (PreparedStatement select = conn.prepareStatement(selectSql)) {
            select.setInt(1, memberId);
            try (ResultSet rs = select.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Member not found.");
                    return;
                }

                // Use current details if new ones are blank
                String currentFname = rs.getString("fname");
                String currentLname = rs.getString("lname");
                String currentEmail = rs.getString("email");
                String currentPhone = rs.getString("phone_number");

                if (fname.isEmpty()) fname = currentFname;
                if (lname.isEmpty()) lname = currentLname;
                if (email.isEmpty()) email = currentEmail;
                if (phone.isEmpty()) phone = currentPhone;

                // Execute update
                try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                    update.setString(1, fname);
                    update.setString(2, lname);
                    update.setString(3, email);
                    update.setString(4, phone);
                    update.setInt(5, memberId);
                    int rows = update.executeUpdate();
                    if (rows > 0) {
                        System.out.println("Profile updated successfully.");
                    } else {
                        System.out.println("No rows updated.");
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error updating personal details:");
            e.printStackTrace();
        }
    }

    // Profile Management: Update fitness goals (creates new entry)
    private static void updateFitnessGoals(int memberId) {

        // Prompt for new goals
        System.out.println("\n=== Update Fitness Goals ===");
        System.out.print("Enter weight goal (kg): ");
        double weightGoal = readDouble();

        System.out.print("Enter VO2 max goal: ");
        double vo2Goal = readDouble();

        System.out.print("Enter body fat percentage goal: ");
        double bodyFatGoal = readDouble();

        // Insert new goals snapshot query
        String sql = """
                INSERT INTO FitnessGoals (member_id, timestamp, weight_goal, vo2_max_goal, body_fat_percentage_goal)
                VALUES (?, NOW(), ?, ?, ?)
                """;

        // Establish a connection
        Connection conn = DatabaseManager.getConnection();

        // injecting values into the query
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            ps.setDouble(2, weightGoal);
            ps.setDouble(3, vo2Goal);
            ps.setDouble(4, bodyFatGoal);

            // executing the query
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("Fitness goals updated (new snapshot saved).");
            } else {
                System.out.println("No rows inserted (something went wrong).");
            }
        } catch (SQLException e) {
            System.out.println("Error updating fitness goals:");
            e.printStackTrace();
        }
    }


    // Health History: Add new health metric (creates new entry)
    private static void addHealthMetric(int memberId) {

        // Prompt for new health metric
        System.out.println("\n=== Add Health Metric ===");
        System.out.print("Enter height (cm): ");
        double height = readDouble();

        System.out.print("Enter weight (kg): ");
        double weight = readDouble();

        System.out.print("Enter VO2 max: ");
        double vo2 = readDouble();

        System.out.print("Enter body fat percentage: ");
        double bodyFat = readDouble();

        // Insert new health metric query
        String sql = """
                INSERT INTO HealthMetric (member_id, timestamp, height, weight, vo2_max, body_fat_percentage)
                VALUES (?, NOW(), ?, ?, ?, ?)
                """;

        // Establish a connection
        Connection conn = DatabaseManager.getConnection();

        // injecting values into the query
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            ps.setDouble(2, height);
            ps.setDouble(3, weight);
            ps.setDouble(4, vo2);
            ps.setDouble(5, bodyFat);

            // executing the query
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("Health metric added (new entry in history).");
            } else {
                System.out.println("No rows inserted.");
            }
        } catch (SQLException e) {
            System.out.println("Error adding health metric:");
            e.printStackTrace();
        }
    }

    // Group Class Registration: Register for scheduled classes if capacity permits
    private static void registerForClass(int memberId) {

        // Establish a connection
        Connection conn = DatabaseManager.getConnection();

        // List available classes with current registration counts
        System.out.println("\n=== Available Classes ===");
        String listSql = """
                SELECT fc.id, fc.date, fc.start_hour, fc.end_hour, fc.capacity, COUNT(r.member_id) AS current_registrations
                FROM FitnessClass fc
                LEFT JOIN Registers r ON fc.id = r.fitness_class_id
                GROUP BY  fc.id, fc.date, fc.start_hour, fc.end_hour, fc.capacity
                ORDER BY fc.date, fc.start_hour;
                """;

        // Display classes
        try (PreparedStatement ps = conn.prepareStatement(listSql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int classId = rs.getInt("id");
                int capacity = rs.getInt("capacity");
                int current = rs.getInt("current_registrations");
                int spotsLeft = capacity - current;

                System.out.printf(
                        "Class ID: %d | Date: %s | %s–%s | Capacity: %d | Registered: %d | Spots left: %d%n", classId,  rs.getDate("date"),
                        rs.getTime("start_hour"), rs.getTime("end_hour"), capacity,  current, spotsLeft
                );
            }
        } catch (SQLException e) {
            System.out.println("Error listing classes:");
            e.printStackTrace();
            return;
        }

        // Prompt for class ID to register
        System.out.print("\nEnter the Class ID you want to register for: ");
        int classId = readInt();

        // Check capacity before registering
        String capacitySql = """
                SELECT fc.capacity, COUNT(r.member_id) AS current_registrations
                FROM FitnessClass fc
                LEFT JOIN Registers r ON fc.id = r.fitness_class_id
                WHERE fc.id = ?
                GROUP BY fc.capacity;
                """;


        try (PreparedStatement ps = conn.prepareStatement(capacitySql)) {
            ps.setInt(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Class not found.");
                    return;
                }

                // Checking capacity
                int capacity = rs.getInt("capacity");
                int current = rs.getInt("current_registrations");
                if (current >= capacity) {
                    System.out.println("Class is full. Cannot register.");
                    return;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error checking capacity:");
            e.printStackTrace();
            return;
        }

        // Register the member for the class
        String insertSql = "INSERT INTO Registers (member_id, fitness_class_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setInt(1, memberId);
            ps.setInt(2, classId);
            ps.executeUpdate();
            System.out.println("Successfully registered for class.");
        } catch (SQLException e) {
            System.out.println("Error registering for class:");
            System.out.println("DB message: " + e.getMessage());
        }
    }


    // Nice to have helper: View registered classes
    private static void viewRegisteredClasses(int memberId) {
        System.out.println("\n=== Your Registered Classes ===");

        String sql = """
                SELECT fitness_class_id, date, start_hour, end_hour, room_location, trainer_fname, trainer_lname
                 FROM MemberClassSchedule
                WHERE member_id = ?
                 ORDER BY date, start_hour;
                """;

        // Establish a connection
        Connection conn = DatabaseManager.getConnection();
        // injecting values into the query
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            // executing the query
            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.printf(
                            "Class ID: %d | Date: %s | %s–%s | Room: %s | Trainer: %s %s%n",  rs.getInt("fitness_class_id"),
                            rs.getDate("date"), rs.getTime("start_hour"), rs.getTime("end_hour"),
                            rs.getString("room_location"), rs.getString("trainer_fname"), rs.getString("trainer_lname")
                    );
                }
                if (!any) {
                    System.out.println("You are not registered in any classes.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching registered classes:");
            e.printStackTrace();
        }
    }


    // Nice to have: View health history
    private static void viewHealthHistory(int memberId) {
        System.out.println("\n=== Health History (last 5 entries) ===");

        String sql = """
                SELECT timestamp, height, weight, vo2_max, body_fat_percentage
                FROM HealthMetric
                WHERE member_id = ?
                ORDER BY timestamp DESC
                 LIMIT 5
                """;

        // Establish a connection
        Connection conn = DatabaseManager.getConnection();
        // injecting values into the query
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            // executing the query
            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.printf(
                            "At %s | Height: %.1f cm | Weight: %.1f kg | VO2: %.1f | Body fat: %.1f%%%n", rs.getTimestamp("timestamp"),
                            rs.getDouble("height"), rs.getDouble("weight"), rs.getDouble("vo2_max"),
                            rs.getDouble("body_fat_percentage")
                    );
                }
                if (!any) {
                    System.out.println("No health metrics recorded yet.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching health history:");
            e.printStackTrace();
        }
    }


    // Input helpers
    private static int readInt() {

        // Validate integer input
        while (!sc.hasNextInt()) {
            sc.next();
            System.out.print("Please enter a number: ");
        }
        // Read the integer value
        int value = sc.nextInt();
        sc.nextLine(); // consume newline
        return value;
    }

    private static double readDouble() {

        // Validate double input
        while (!sc.hasNextDouble()) {
            sc.next();
            System.out.print("Please enter a valid number: ");
        }
        // Read the double value
        double value = sc.nextDouble();
        sc.nextLine(); // consume newline
        return value;
    }
}
