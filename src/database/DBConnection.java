package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection — centralises all JDBC connection logic for the
 * application. Every controller obtains its Connection through
 * getConnection() rather than opening one directly, so the database
 * credentials and driver setup live in exactly one place.
 */
public class DBConnection {

    // ===== Database Configuration =====
    private static final String URL = "jdbc:mysql://localhost:3306/tourism_db";
    private static final String USER = "root";
    private static final String PASSWORD = "Dilhanya2008#"; // Change this to your MySQL password

    // ===== Get Database Connection =====
    public static Connection getConnection() {
        try {
            // Load MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Create and return connection
            return DriverManager.getConnection(URL, USER, PASSWORD);

        } catch (ClassNotFoundException e) {
            System.out.println("MySQL JDBC Driver not found.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("Database connection failed.");
            e.printStackTrace();
        }

        return null;
    }

    // ===== Test Database Connection =====
    public static boolean testConnection() {
        try (Connection con = getConnection()) {
            
            //create the connection != if the connection open?
            if (con != null && !con.isClosed()) {
                System.out.println("Database connected successfully!");
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();//Error details showing the console
        }

        System.out.println("Database connection failed!");
        return false;
    }
}