package app.actions;

import app.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class RegistrationActions {

    private static final Scanner sc = new Scanner(System.in);

    // User Registration Member Operation
    public static void registerNewMember() {
        System.out.println("\n=== New Member Registration ===");

        // Data collection
        System.out.print("First name: ");
        String fname = sc.nextLine().trim();

        System.out.print("Last name: ");
        String lname = sc.nextLine().trim();

        System.out.print("Date of birth (YYYY-MM-DD): ");
        String dob = sc.nextLine().trim();


        System.out.print("Gender (optional): ");
        String gender = sc.nextLine().trim();
        if (gender.isEmpty()) gender = null;

        System.out.print("Email (can be shared): ");
        String email = sc.nextLine().trim();

        System.out.print("Phone number (optional): ");
        String phone = sc.nextLine().trim();
        if (phone.isEmpty()) phone = null;

        // Get a connection
        Connection conn = DatabaseManager.getConnection();

        // Insert new member into the database and get the generated ID
        String insertSql = """
                INSERT INTO Member (fname, lname, date_of_birth, gender, email, phone_number)
                VALUES (?, ?, ?, ?, ?, ?)
                 RETURNING id
                """;

        // inject data and execute
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setString(1, fname);
            ps.setString(2, lname);
            ps.setDate(3, java.sql.Date.valueOf(dob));
            if (gender == null) {
                ps.setNull(4, java.sql.Types.VARCHAR);
            } else {
                ps.setString(4, gender);
            }
            ps.setString(5, email);
            if (phone == null) {
                ps.setNull(6, java.sql.Types.VARCHAR);
            } else {
                ps.setString(6, phone);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int newId = rs.getInt("id");
                    System.out.println("Registration successful! Your Member ID is: " + newId);
                    System.out.println("Use this ID to log in as a member.");
                } else {
                    System.out.println("Registration failed (no ID returned).");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error inserting new member:");
            e.printStackTrace();
        }
    }
}
