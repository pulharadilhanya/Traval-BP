package model;

/** Report — a snapshot summary of system activity, shown on the Admin Reports tab. */
public class Report {
    private int totalUsers;
    private int totalTours;
    private int totalBookings;
    private int confirmedBookings;
    private int pendingBookings;
    private int totalReservations;

    public Report() {}

    public Report(int totalUsers, int totalTours, int totalBookings,
                   int confirmedBookings, int pendingBookings, int totalReservations) {
        this.totalUsers = totalUsers;
        this.totalTours = totalTours;
        this.totalBookings = totalBookings;
        this.confirmedBookings = confirmedBookings;
        this.pendingBookings = pendingBookings;
        this.totalReservations = totalReservations;
    }

    public int getTotalUsers() { return totalUsers; }
    public int getTotalTours() { return totalTours; }
    public int getTotalBookings() { return totalBookings; }
    public int getConfirmedBookings() { return confirmedBookings; }
    public int getPendingBookings() { return pendingBookings; }
    public int getTotalReservations() { return totalReservations; }

    public void generateReport(int totalUsers, int totalTours, int totalBookings,
                                int confirmedBookings, int pendingBookings, int totalReservations) {
        this.totalUsers = totalUsers;
        this.totalTours = totalTours;
        this.totalBookings = totalBookings;
        this.confirmedBookings = confirmedBookings;
        this.pendingBookings = pendingBookings;
        this.totalReservations = totalReservations;
    }

    public String viewReport() {
        return "Users:"+totalUsers+" | Tours:"+totalTours+" | Bookings:"+totalBookings+
               " (Confirmed:"+confirmedBookings+", Pending:"+pendingBookings+")"+
               " | Reservations:"+totalReservations;
    }
}
