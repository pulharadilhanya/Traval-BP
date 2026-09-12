package controller;

import database.DBConnection;

import model.Attendance;
import model.Tour;
import java.sql.*;
import java.util.*;

/**
 * TourGuideController — what a Tour Guide does day to day: see the
 * tours, update their progress status, and mark tourist attendance.
 */
public class TourGuideController {
    private final TourController tourController = new TourController();

    /** Returns every tour, used by the Tour Guide dashboard's Assigned Tours screen. */
    public List<Tour> getAssignedTours() {
        return tourController.getAllTours();
    }

    /** Matches the search box on the Assigned Tours screen against name, destination or status. */
    public List<Tour> searchAssignedTours(String q) {
        return tourController.searchTours(q);
    }

    /** Updates a tour's progress status. */
    public boolean updateTourStatus(int tourId, String status) {
        return tourController.updateStatus(tourId, status);
    }

    /** Marks a tourist present/absent for a tour on today's date. */
    public boolean markAttendance(int tourId, String touristName, boolean present) {
        try (Connection c = DBConnection.getConnection()) {
            if (c == null) return false;
            PreparedStatement ps = c.prepareStatement(
                "INSERT INTO attendance(tourId,touristName,date,present) VALUES(?,?,?,?)");
            ps.setInt(1, tourId);
            ps.setString(2, touristName);
            ps.setString(3, java.time.LocalDate.now().toString());
            ps.setBoolean(4, present);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { System.out.println(e); return false; }
    }

    /** Deletes an attendance record by id. */
    public boolean deleteAttendance(int attendanceId) {
        try (Connection c = DBConnection.getConnection()) {
            PreparedStatement ps = c.prepareStatement("DELETE FROM attendance WHERE attendanceId=?");
            ps.setInt(1, attendanceId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { return false; }
    }

    /** Updates an existing attendance record's tour, tourist name and present status. */
    public boolean updateAttendance(int attendanceId, int tourId, String touristName, boolean present) {
        try (Connection c = DBConnection.getConnection()) {
            PreparedStatement ps = c.prepareStatement(
                "UPDATE attendance SET tourId=?, touristName=?, present=? WHERE attendanceId=?");
            ps.setInt(1, tourId);
            ps.setString(2, touristName);
            ps.setBoolean(3, present);
            ps.setInt(4, attendanceId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { System.out.println(e); return false; }
    }

    /** Returns the attendance records for a single tour. */
    public List<Attendance> getAttendanceForTour(int tourId) {
        List<Attendance> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT attendanceId,tourId,touristName,date,present FROM attendance WHERE tourId=? ORDER BY attendanceId DESC");
            ps.setInt(1, tourId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Attendance(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getBoolean(5)));
            }
        } catch (Exception e) { System.out.println(e); }
        return list;
    }

    /** Returns every attendance record across all tours. */
    public List<Attendance> getAllAttendance() {
        List<Attendance> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection()) {
            ResultSet rs = c.createStatement().executeQuery(
                "SELECT attendanceId,tourId,touristName,date,present FROM attendance ORDER BY attendanceId DESC");
            while (rs.next()) {
                list.add(new Attendance(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getBoolean(5)));
            }
        } catch (Exception e) { System.out.println(e); }
        return list;
    }

    /** Matches the search box on the Attendance screen against tourist name or tour date. */
    public List<Attendance> searchAttendance(String q) {
        List<Attendance> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT attendanceId,tourId,touristName,date,present FROM attendance " +
                "WHERE touristName LIKE ? OR date LIKE ? ORDER BY attendanceId DESC");
            String like = "%"+q+"%";
            ps.setString(1, like); ps.setString(2, like);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Attendance(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getBoolean(5)));
            }
        } catch (Exception e) { System.out.println(e); }
        return list;
    }
}
