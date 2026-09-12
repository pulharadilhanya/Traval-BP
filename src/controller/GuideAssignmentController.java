package controller;

import database.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * GuideAssignmentController — backs the Tour Manager's "Assign Tour Guide"
 * screen.
 *
 * Talks to a new `guide_assignments` table (guideId, tourId, tourDate,
 * notes, status) that isn't part of the original schema, so it needs to be
 * created once — see the accompanying guide_assignments_table.sql.
 *
 * A guide is just a `users` row with role = 'TourGuide' — there's no
 * separate guides table, so getAvailableGuides() reads straight off users.
 *
 * Rows come back as Object[] the same way ScheduleController/BookingController
 * do: {assignmentId, guideId, guideName, tourId, tourName, tourDate, notes, status}.
 *
 * Note: only import java.sql.* here (not java.util.*) — mixing the two
 * wildcard imports is exactly what caused the ambiguous "Date" compile
 * error in ScheduleController earlier, so this file avoids that on purpose.
 */
public class GuideAssignmentController {

    /** Assigns a tour guide to a tour on a given date. */
    public boolean assignGuide(int guideId, int tourId, String tourDate, String notes, String status){
        try (Connection c=DBConnection.getConnection()){
            if (c == null) return false;
            PreparedStatement ps=c.prepareStatement(
              "INSERT INTO guide_assignments(guideId,tourId,tourDate,notes,status) VALUES(?,?,?,?,?)");
            ps.setInt(1,guideId);
            ps.setInt(2,tourId);
            ps.setDate(3, java.sql.Date.valueOf(tourDate));
            ps.setString(4,notes);
            ps.setString(5,status);
            return ps.executeUpdate()>0;
        } catch(Exception e){ System.out.println(e); return false; }
    }

    /** Updates an existing guide assignment by id. */
    public boolean updateAssignment(int id, int guideId, int tourId, String tourDate, String notes, String status){
        try (Connection c=DBConnection.getConnection()){
            if (c == null) return false;
            PreparedStatement ps=c.prepareStatement(
              "UPDATE guide_assignments SET guideId=?,tourId=?,tourDate=?,notes=?,status=? WHERE assignmentId=?");
            ps.setInt(1,guideId);
            ps.setInt(2,tourId);
            ps.setDate(3, java.sql.Date.valueOf(tourDate));
            ps.setString(4,notes);
            ps.setString(5,status);
            ps.setInt(6,id);
            return ps.executeUpdate()>0;
        } catch(Exception e){ System.out.println(e); return false; }
    }

    /** Removes a guide assignment by id. */
    public boolean deleteAssignment(int id){
        try (Connection c=DBConnection.getConnection()){
            if (c == null) return false;
            PreparedStatement ps=c.prepareStatement("DELETE FROM guide_assignments WHERE assignmentId=?");
            ps.setInt(1,id);
            return ps.executeUpdate()>0;
        } catch(Exception e){ System.out.println(e); return false; }
    }

    /** Returns every guide assignment joined with guide/tour details. */
    public List<Object[]> getAll(){
        List<Object[]> list=new ArrayList<>();
        try (Connection c=DBConnection.getConnection()){
            if (c == null) return list;
            ResultSet rs=c.createStatement().executeQuery(
              "SELECT a.assignmentId, a.guideId, u.username, a.tourId, t.name, a.tourDate, a.notes, a.status " +
              "FROM guide_assignments a " +
              "LEFT JOIN users u ON a.guideId = u.userId " +
              "LEFT JOIN tours t ON a.tourId = t.tourId " +
              "ORDER BY a.tourDate ASC");
            while(rs.next()) list.add(rowFrom(rs));
        } catch(Exception e){ System.out.println(e); }
        return list;
    }

    /** Matches the search box on the Assign Tour Guide screen against guide name, tour name or status. */
    public List<Object[]> search(String q){
        List<Object[]> list=new ArrayList<>();
        try (Connection c=DBConnection.getConnection()){
            if (c == null) return list;
            PreparedStatement ps=c.prepareStatement(
              "SELECT a.assignmentId, a.guideId, u.username, a.tourId, t.name, a.tourDate, a.notes, a.status " +
              "FROM guide_assignments a " +
              "LEFT JOIN users u ON a.guideId = u.userId " +
              "LEFT JOIN tours t ON a.tourId = t.tourId " +
              "WHERE u.username LIKE ? OR t.name LIKE ? OR a.status LIKE ? " +
              "ORDER BY a.tourDate ASC");
            String like = "%"+q+"%";
            ps.setString(1,like); ps.setString(2,like); ps.setString(3,like);
            ResultSet rs=ps.executeQuery();
            while(rs.next()) list.add(rowFrom(rs));
        } catch(Exception e){ System.out.println(e); }
        return list;
    }

    /** Every user account with role = TourGuide, for the "Select Guide" dropdown. */
    public List<Object[]> getAvailableGuides(){
        List<Object[]> list=new ArrayList<>();
        try (Connection c=DBConnection.getConnection()){
            if (c == null) return list;
            ResultSet rs=c.createStatement().executeQuery(
              "SELECT userId, username, contact FROM users WHERE role='TourGuide'");
            while(rs.next()) list.add(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3)});
        } catch(Exception e){ System.out.println(e); }
        return list;
    }

    private Object[] rowFrom(ResultSet rs) throws SQLException {
        return new Object[]{
            rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getInt(4), rs.getString(5),
            rs.getDate(6)==null?"":rs.getDate(6).toString(),
            rs.getString(7), rs.getString(8)
        };
    }
}
