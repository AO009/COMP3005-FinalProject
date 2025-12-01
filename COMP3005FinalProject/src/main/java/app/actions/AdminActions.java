package app.actions;

import app.db.DatabaseManager;
import app.menu.MenuPrinter;

import java.sql.*;
import java.util.Scanner;

public class AdminActions {

    private static final Scanner sc = new Scanner(System.in);

    // Control menu loop
    public static void runMenu(int adminId) {
        while (true) {
            int choice = MenuPrinter.showAdminMenu();
            switch (choice) {
                case 1 -> createClass(adminId); // corresponds to Room Booking and Class management operations
                case 2 -> updateClass(adminId); // corresponds to Room Booking and Class management operations
                case 3 -> viewAllClasses(); // herlper to view all classes
                case 4 -> viewMemberClassSchedule(); // corresponds to the 'View' requirement for project
                case 5 -> cancelClass(adminId); // corresponds to Room Booking and Class management operations
                case 0 -> {
                    System.out.println("Logging out...\n");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }


    // Create Class function
    private static void createClass(int adminId) {
        Connection conn = DatabaseManager.getConnection();

        System.out.println("\n=== Create New Class ===");

        // Begin by showing trainers + their availabilities to help admin choose
        System.out.println("\nTrainers and their availabilities:");
        String trainerAvailSql = """
                   SELECT tr.id AS trainer_id,
                   tr.fname,
                   tr.lname,
                   ia.date,
                   ia.start_hour,
                   ia.end_hour
                   FROM Trainer tr
                   LEFT JOIN IndividualAvailability ia
                   ON ia.trainer_id = tr.id
                   ORDER BY tr.id, ia.date, ia.start_hour
                   """;

        try (PreparedStatement ps = conn.prepareStatement(trainerAvailSql);
             ResultSet rs = ps.executeQuery()) {

            int lastTrainerId = -1;
            boolean hasAny = false;

            while (rs.next()) {
                hasAny = true;
                int trainerId = rs.getInt("trainer_id");
                String fname = rs.getString("fname");
                String lname = rs.getString("lname");
                Date date = rs.getDate("date");
                Time start = rs.getTime("start_hour");
                Time end   = rs.getTime("end_hour");

                if (trainerId != lastTrainerId) {
                    System.out.printf("%nTrainer %d: %s %s%n", trainerId, fname, lname);
                    lastTrainerId = trainerId;
                }

                if (date != null) {
                    System.out.printf("  Availability: %s | %s–%s%n",
                            date,
                            start,
                            end);
                } else {

                    System.out.println("  (No availability defined yet)");
                }
            }

            if (!hasAny) {
                System.out.println("No trainers found.");
            }
        } catch (SQLException e) {
            System.out.println("Error fetching trainers/availabilities:");
            e.printStackTrace();
            return;
        }

        // Show rooms and existing bookings to help admin choose
        System.out.println("\nRooms and existing bookings:");
        String roomBookingsSql = """
            SELECT r.id AS room_id,
            r.location,
            fc.id AS class_id,
             fc.date,
            fc.start_hour,
            fc.end_hour
            FROM Room r
            LEFT JOIN FitnessClass fc
            ON fc.room_id = r.id
            ORDER BY r.id, fc.date, fc.start_hour
            """;

        try (PreparedStatement ps = conn.prepareStatement(roomBookingsSql);
             ResultSet rs = ps.executeQuery()) {

            int lastRoomId = -1;
            boolean hasAnyRoom = false;

            while (rs.next()) {
                hasAnyRoom = true;
                int roomId = rs.getInt("room_id");
                String location = rs.getString("location");
                Integer classId = (Integer) rs.getObject("class_id");
                Date date = rs.getDate("date");
                Time start = rs.getTime("start_hour");
                Time end   = rs.getTime("end_hour");

                if (roomId != lastRoomId) {
                    System.out.printf("%nRoom %d: %s%n", roomId, location);
                    lastRoomId = roomId;
                }

                if (classId != null) {
                    System.out.printf("  Class %d | %s | %s–%s%n", classId, date, start, end);

                } else {
                    System.out.println("  (No bookings yet)");
                }
            }

            if (!hasAnyRoom) {
                System.out.println("No rooms found.");
            }
        } catch (SQLException e) {
            System.out.println("Error fetching rooms/bookings:");
            e.printStackTrace();
            return;
        }

        // Now gather inputs for new class
        System.out.println("\nEnter class details:");
        System.out.print("Date (YYYY-MM-DD): ");
        String dateStr = sc.nextLine().trim();

        System.out.print("Start time (HH:MM, 24h): ");
        String startStr = sc.nextLine().trim();

        System.out.print("End time (HH:MM, 24h): ");
        String endStr = sc.nextLine().trim();

        System.out.print("Capacity: ");
        int capacity = readInt();

        System.out.print("Trainer ID: ");
        int trainerId = readInt();

        System.out.print("Room ID: ");
        int roomId = readInt();

        Date date;
        Time startTime;
        Time endTime;
        try {
            date = Date.valueOf(dateStr);
            startTime = Time.valueOf(startStr + ":00");
            endTime   = Time.valueOf(endStr + ":00");
        } catch (IllegalArgumentException e) {

            // invalid date/time format
            System.out.println("Invalid date or time format.");
            return;
        }


        String insertSql = """
            INSERT INTO FitnessClass (capacity, date, start_hour, end_hour, room_id, trainer_id, admin_id)
             VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setInt(1, capacity);
            ps.setDate(2, date);
            ps.setTime(3, startTime);
            ps.setTime(4, endTime);
            ps.setInt(5, roomId);
            ps.setInt(6, trainerId);
            ps.setInt(7, adminId);

            ps.executeUpdate();
            System.out.println("Class created successfully.");
        } catch (SQLException e) {
            System.out.println("Error creating class:");
            // Triggers complain here if room/trainer is double-booked or trainer not available
            System.out.println("Error message: " + e.getMessage());
        }
    }


    // Admin update class as needed
    private static void updateClass(int adminId) {

        // Establishing a database connection
        Connection conn = DatabaseManager.getConnection();

        System.out.println("\n=== Update Existing Class ===");

        // Show all classes to decide which to update
        viewAllClasses();

        System.out.print("\nEnter Class ID to update (or 0 to cancel): ");
        int classId = readInt();
        if (classId == 0) {
            System.out.println("Cancelled.");
            return;
        }

        // Create a SELECT querey to fetch current class details
        String selectSql = """
                SELECT id, capacity, date, start_hour, end_hour, room_id, trainer_id
                FROM FitnessClass
                 WHERE id = ?
                """;

        // Load ID value
        try (PreparedStatement select = conn.prepareStatement(selectSql)) {
            select.setInt(1, classId);


            try (ResultSet rs = select.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Class not found.");
                    return;
                }

                // Gathering current values and asking for new ones to update
                int currentCapacity = rs.getInt("capacity");
                Date currentDate    = rs.getDate("date");
                Time currentStart   = rs.getTime("start_hour");
                Time currentEnd     = rs.getTime("end_hour");
                int currentRoomId   = rs.getInt("room_id");
                int currentTrainerId= rs.getInt("trainer_id");

                System.out.println("\nCurrent values (press Enter to keep):");

                System.out.printf("Capacity [%d]: ", currentCapacity);
                String capStr = sc.nextLine().trim();

                System.out.printf("Date [%s] (YYYY-MM-DD): ", currentDate);
                String dateStr = sc.nextLine().trim();

                System.out.printf("Start time [%s] (HH:MM): ", currentStart.toString().substring(0,5));
                String startStr = sc.nextLine().trim();

                System.out.printf("End time [%s] (HH:MM): ", currentEnd.toString().substring(0,5));
                String endStr = sc.nextLine().trim();

                System.out.printf("Room ID [%d]: ", currentRoomId);
                String roomStr = sc.nextLine().trim();

                System.out.printf("Trainer ID [%d]: ", currentTrainerId);
                String trainerStr = sc.nextLine().trim();

                // Process inputs, keeping current values if input is empty and validating new inputs
                int newCapacity = currentCapacity;
                if (!capStr.isEmpty()) {
                    try {
                        newCapacity = Integer.parseInt(capStr);
                        if (newCapacity <= 0) {
                            System.out.println("Capacity must be positive.");
                            return;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid capacity value.");
                        return;
                    }
                }

                Date newDate = currentDate;
                if (!dateStr.isEmpty()) {
                    try {
                        newDate = Date.valueOf(dateStr);
                    } catch (IllegalArgumentException e) {
                        System.out.println("Invalid date format.");
                        return;
                    }
                }

                Time newStart = currentStart;
                if (!startStr.isEmpty()) {
                    try {
                        newStart = Time.valueOf(startStr + ":00");
                    } catch (IllegalArgumentException e) {
                        System.out.println("Invalid start time.");
                        return;
                    }
                }

                Time newEnd = currentEnd;
                if (!endStr.isEmpty()) {
                    try {
                        newEnd = Time.valueOf(endStr + ":00");
                    } catch (IllegalArgumentException e) {
                        System.out.println("Invalid end time.");
                        return;
                    }
                }

                int newRoomId = currentRoomId;
                if (!roomStr.isEmpty()) {
                    try {
                        newRoomId = Integer.parseInt(roomStr);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid room ID.");
                        return;
                    }
                }


                int newTrainerId = currentTrainerId;
                if (!trainerStr.isEmpty()) {
                    try {
                        newTrainerId = Integer.parseInt(trainerStr);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid trainer ID.");
                        return;
                    }
                }


                // crafting the UPDATE query
                String updateSql = """
                        UPDATE FitnessClass
                        SET capacity = ?,date = ?, start_hour = ?,  end_hour = ?, room_id = ?, trainer_id = ?
                        WHERE id = ?
                        """;

                // injecting the UPDATE query
                try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                    update.setInt(1, newCapacity);
                    update.setDate(2, newDate);
                    update.setTime(3, newStart);
                    update.setTime(4, newEnd);
                    update.setInt(5, newRoomId);
                    update.setInt(6, newTrainerId);
                    update.setInt(7, classId);

                    // executing the UPDATE query
                    update.executeUpdate();
                    System.out.println("Class updated successfully.");
                } catch (SQLException e) {
                    System.out.println("Error updating class:");
                    System.out.println("DB message: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            System.out.println("Error loading class for update:");
            e.printStackTrace();
        }
    }

    // Allowing the admin to view all classes
    private static void viewAllClasses() {
        System.out.println("\n=== All Classes ===");

        // Creating the SELECT query
        String sql = """
            SELECT fc.id, fc.date,  fc.start_hour, fc.end_hour, fc.capacity,fc.room_id, fc.trainer_id, r.location AS room_location,
            t.fname AS trainer_fname, t.lname AS trainer_lname
            FROM FitnessClass fc
            JOIN Room r ON fc.room_id = r.id
            JOIN Trainer t  ON fc.trainer_id = t.id
            ORDER BY fc.date, fc.start_hour
            """;

        // Establishing a connection
        Connection conn = DatabaseManager.getConnection();

        //  Executing the SELECT query and printing results
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf( "Class %d | %s | %s–%s | Cap: %d | Room %d (%s) | Trainer %d (%s %s)%n",  rs.getInt("id"),
                        rs.getDate("date"),  rs.getTime("start_hour"),  rs.getTime("end_hour"),
                        rs.getInt("capacity"),  rs.getInt("room_id"),  rs.getString("room_location"),
                        rs.getInt("trainer_id"), rs.getString("trainer_fname"), rs.getString("trainer_lname")
                );
            }
            if (!any) {
                System.out.println("No classes defined yet.");
            }
        } catch (SQLException e) {
            System.out.println("Error fetching classes:");
            e.printStackTrace();
        }
    }


    // View MemberClassSchedule view (master view essentialy display all data and connections using a view)
    private static void viewMemberClassSchedule() {
        System.out.println("\n=== MemberClassSchedule (VIEW) ===");

        // view is called MemberClassSchedule
        String sql = """
                SELECT member_id, member_fname, member_lname,
                 fitness_class_id, date, start_hour, end_hour,
                room_location, trainer_fname, trainer_lname
                FROM MemberClassSchedule
                ORDER BY date, start_hour, member_id
                """;

        // Establishing a connection
        Connection conn = DatabaseManager.getConnection();

        // Executing the SELECT query and printing results
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("Member %d (%s %s) -> Class %d | %s | %s–%s | Room: %s | Trainer: %s %s%n",  rs.getInt("member_id"),
                        rs.getString("member_fname"), rs.getString("member_lname"), rs.getInt("fitness_class_id"),
                        rs.getDate("date"), rs.getTime("start_hour"), rs.getTime("end_hour"),
                        rs.getString("room_location"), rs.getString("trainer_fname"), rs.getString("trainer_lname")
                );
            }
            if (!any) {
                System.out.println("No registrations yet.");
            }
        } catch (SQLException e) {
            System.out.println("Error fetching MemberClassSchedule:");
            e.printStackTrace();
        }
    }

    // Cancel class function part of admin actions
    private static void cancelClass(int adminId) {

        // Establishing a database connection
        Connection conn = DatabaseManager.getConnection();

        System.out.println("\n=== Cancel Class / Session ===");

        // Show all classes to decide which to cancel makes use of viewAllClasses
        viewAllClasses();

        // Prompt for class ID to cancel
        System.out.print("\nEnter Class ID to cancel (or 0 to abort): ");
        int classId = readInt();
        if (classId == 0) {
            System.out.println("Cancelled.");
            return;
        }

        // Check it exists
        String checkSql = "SELECT id, date, start_hour, end_hour FROM FitnessClass WHERE id = ?";
        try (PreparedStatement check = conn.prepareStatement(checkSql)) {
            check.setInt(1, classId);
            try (ResultSet rs = check.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Class not found.");
                    return;
                }
                System.out.printf("Cancelling Class %d on %s %s–%s.%n",  rs.getInt("id"), rs.getDate("date"),
                        rs.getTime("start_hour"),  rs.getTime("end_hour"));
            }
        } catch (SQLException e) {
            System.out.println("Error checking class:");
            e.printStackTrace();
            return;
        }

        // Deleting the class
        String deleteSql = "DELETE FROM FitnessClass WHERE id = ?";
        try (PreparedStatement del = conn.prepareStatement(deleteSql)) {
            del.setInt(1, classId);
            int rows = del.executeUpdate();
            if (rows > 0) {
                System.out.println("Class cancelled successfully.");
                System.out.println("(Any registrations for this class were removed via ON DELETE CASCADE.)");
            } else {
                System.out.println("No class deleted (maybe it was already removed).");
            }
        } catch (SQLException e) {
            System.out.println("Error cancelling class:");
            e.printStackTrace();
        }
    }

   // Helper to read integer input with validation
    private static int readInt() {
        while (!sc.hasNextInt()) {
            sc.next();
            System.out.print("Please enter a number: ");
        }
        int value = sc.nextInt();
        sc.nextLine(); // consume newline
        return value;
    }
}


