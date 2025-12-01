package app.auth;

import app.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AuthService {

    // Method to check if a member with the given id exists
    public static boolean isValidMember(int id) {
        return exists("SELECT FROM Member WHERE id = ?", id);
    }

    // Method to check if a trainer with the given id exists
    public static boolean isValidTrainer(int id) {
        return exists("SELECT  FROM Trainer WHERE id = ?", id);
    }

    // Method to check if an administration staff with the given id exists
    public static boolean isValidAdmin(int id) {
        return exists("SELECT  FROM AdministrationStaff WHERE id = ?", id);
    }

    // Helper method to execute the existence check query
    private static boolean exists(String sql, int id) {

        // Establishing a connection to the database
        Connection conn = DatabaseManager.getConnection();

        // Injecting the id parameter and executing the query
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                // If a record is found, return true. Otherwise, return false.
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // In case of an exception, return false
            return false;
        }
    }
}
