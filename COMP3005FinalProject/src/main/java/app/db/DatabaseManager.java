package app.db;
import java.sql.*;

public class DatabaseManager {
    private static Connection conn;

    // Method to establish a connection to the database
    public static void connect() {
        try {
            conn = DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/postgres",
                    "postgres",
                    "SalihOmer12"
            );
            conn.setSchema("public");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to retrieve the established database connection
    public static Connection getConnection() {
        return conn;
    }

    // Method to close the database connection
    public static void close() {
        try {
            if (conn != null) conn.close();
        } catch (Exception ignored) {}
    }
}