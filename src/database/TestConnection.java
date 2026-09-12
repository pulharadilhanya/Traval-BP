package database;

/**
 * TestConnection — a small standalone class that can be run on its own
 * (right-click > Run File in NetBeans) to verify the MySQL connection
 * without starting the full Swing application.
 */
public class TestConnection {
    public static void main(String[] args) {
        System.out.println("Testing connection to tourism_db ...");
        boolean ok = DBConnection.testConnection();
        System.out.println(ok ? "Result: SUCCESS" : "Result: FAILED");
    }
}
