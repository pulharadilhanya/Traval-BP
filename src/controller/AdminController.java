package controller;

import database.DBConnection;

import model.Report;
import java.sql.*;
import java.util.*;

/**
 * AdminController — everything the Admin role does that isn't already
 * plain user CRUD (UserController): approving bookings and producing
 * the reports summary.
 */
public class AdminController {
    private final UserController userController = new UserController();
    private final TourController tourController = new TourController();
    private final BookingController bookingController = new BookingController();

    /** Admin approves (or rejects) a pending booking. */
    public boolean approveBooking(int bookingId) {
        return bookingController.updateStatus(bookingId, "Confirmed");
    }

    /** Admin rejects a pending booking. */
    public boolean rejectBooking(int bookingId) {
        return bookingController.updateStatus(bookingId, "Cancelled");
    }

    /** Admin reverts a booking back to Pending (e.g. to re-review it). */
    public boolean pendingBooking(int bookingId) {
        return bookingController.updateStatus(bookingId, "Pending");
    }

    /** Builds a one-shot snapshot report for the Admin > Reports tab. */
    public Report getReport() {
        int totalUsers = userController.getAllUsers().size();
        int totalTours = tourController.getAllTours().size();
        List<Object[]> bookings = bookingController.getAll();
        int totalBookings = bookings.size();
        long confirmed = bookings.stream().filter(r -> "Confirmed".equals(r[6])).count();
        long pending = bookings.stream().filter(r -> "Pending".equals(r[6])).count();
        int totalReservations = getReservationCount();
        return new Report(totalUsers, totalTours, totalBookings, (int) confirmed, (int) pending, totalReservations);
    }

    private int getReservationCount() {
        try (Connection c = DBConnection.getConnection()) {
            if (c == null) return 0;
            ResultSet rs = c.createStatement().executeQuery("SELECT COUNT(*) FROM reservations");
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { System.out.println(e); }
        return 0;
    }
}
