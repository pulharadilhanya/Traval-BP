package model;

/**
 * TourManager — creates and manages tour packages/schedules, and
 * coordinates transport and hotel resources for tours.
 */
public class TourManager extends User {

    public TourManager() { super(); setRole("TourManager"); }

    public TourManager(int id, String username, String password, String email, String contact) {
        super(id, username, password, email, contact, "TourManager");
    }

    @Override
    public String[] getPermissions() {
        return new String[]{
            "ADD_TOUR_PACKAGES", "UPDATE_SCHEDULES", "MANAGE_TRANSPORT_HOTELS", "MONITOR_TOURS"
        };
    }

    @Override
    public String getHomeTitle() {
        return "Tour Manager Dashboard";
    }
}
