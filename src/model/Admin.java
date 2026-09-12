package model;

/**
 * Admin — controls the whole system: manages users, approves bookings,
 * views reports. Extends User so it can be used anywhere a User is
 * expected (polymorphism), while adding role-specific behaviour.
 */
public class Admin extends User {

    public Admin() { super(); setRole("Admin"); }

    public Admin(int id, String username, String password, String email, String contact) {
        super(id, username, password, email, contact, "Admin");
    }

    @Override
    public String[] getPermissions() {
        return new String[]{
            "MANAGE_USERS", "VIEW_REPORTS", "APPROVE_BOOKINGS"
        };
    }

    @Override
    public String getHomeTitle() {
        return "Admin Dashboard";
    }

    public void generateReports() {
        // Admin triggers report generation (see Report class for the generated data)
    }
}
