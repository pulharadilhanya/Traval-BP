package controller;

import database.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * One-time tool: run this ONCE after adding password hashing, to convert
 * any old plain-text passwords already sitting in the users table into
 * the new salt:hash format. Safe to run more than once — rows that are
 * already hashed (contain a ":") are left untouched.
 *
 * How to run it in NetBeans:
 *   1. Right-click this file -> Run File (or Shift+F6)
 *   2. Check the console output for how many rows were updated.
 *   3. Existing users can then log in with THEIR SAME original password.
 */
public class PasswordMigration {

    public static void main(String[] args) {
        int updated = 0, alreadyHashed = 0, failed = 0;

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                System.out.println("Could not connect to the database.");
                return;
            }

            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT userId, password FROM users")) {

                while (rs.next()) {
                    int id = rs.getInt("userId");
                    String storedPassword = rs.getString("password");

                    if (PasswordUtil.isHashed(storedPassword)) {
                        alreadyHashed++;
                        continue;
                    }

                    String newHash = PasswordUtil.hashPassword(storedPassword);
                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE users SET password=? WHERE userId=?")) {
                        ps.setString(1, newHash);
                        ps.setInt(2, id);
                        if (ps.executeUpdate() > 0) {
                            updated++;
                        } else {
                            failed++;
                        }
                    }
                }
            }

            System.out.println("Migration complete.");
            System.out.println("  Updated:        " + updated);
            System.out.println("  Already hashed:  " + alreadyHashed);
            System.out.println("  Failed:          " + failed);

        } catch (Exception e) {
            System.out.println("Migration error: " + e);
        }
    }
}
