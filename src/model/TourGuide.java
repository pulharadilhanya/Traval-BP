package model;

/**
 * TourGuide — guides tourists during a tour: views assigned tours,
 * updates tour status/progress, and manages tourist attendance.
 */
public class TourGuide extends User {

    public TourGuide() { super(); setRole("TourGuide"); }

    public TourGuide(int id, String username, String password, String email, String contact) {
        super(id, username, password, email, contact, "TourGuide");
    }

    @Override
    public String[] getPermissions() {
        return new String[]{
            "VIEW_ASSIGNED_TOURS", "UPDATE_TOUR_STATUS", "MANAGE_ATTENDANCE", "PROVIDE_TRAVEL_INFO"
        };
    }

    @Override
    public String getHomeTitle() {
        return "Tour Guide Dashboard";
    }
}
