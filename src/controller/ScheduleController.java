package controller;

import database.DBConnection;

import java.sql.*;
import java.util.*;

/**
 * ScheduleController — backs the Tour Manager's "Update Schedule" screen.
 *
 * Talks to a new `schedules` table (tourId, tourDate, startTime, endTime,
 * location, notes, status) that isn't part of the original schema, so it
 * needs to be created once — see the accompanying schedules_table.sql.
 *
 * Rows come back as Object[] the same way BookingController/UserController
 * already do elsewhere in this app: {scheduleId, tourId, tourName, tourDate,
 * startTime, endTime, location, notes, status}.
 */
public class ScheduleController {

    public boolean addSchedule(int tourId, String tourDate, String startTime, String endTime,
                                String location, String notes, String status){
        try (Connection c=DBConnection.getConnection()){
            if (c == null) return false;
            PreparedStatement ps=c.prepareStatement(
              "INSERT INTO schedules(tourId,tourDate,startTime,endTime,location,notes,status) VALUES(?,?,?,?,?,?,?)");
            ps.setInt(1,tourId);
            ps.setDate(2, java.sql.Date.valueOf(tourDate));
            ps.setTime(3, Time.valueOf(normalizeTime(startTime)));
            ps.setTime(4, Time.valueOf(normalizeTime(endTime)));
            ps.setString(5,location);
            ps.setString(6,notes);
            ps.setString(7,status);
            return ps.executeUpdate()>0;
        } catch(Exception e){ System.out.println(e); return false; }
    }

    public boolean updateSchedule(int id, int tourId, String tourDate, String startTime, String endTime,
                                   String location, String notes, String status){
        try (Connection c=DBConnection.getConnection()){
            if (c == null) return false;
            PreparedStatement ps=c.prepareStatement(
              "UPDATE schedules SET tourId=?,tourDate=?,startTime=?,endTime=?,location=?,notes=?,status=? WHERE scheduleId=?");
            ps.setInt(1,tourId);
            ps.setDate(2, java.sql.Date.valueOf(tourDate));
            ps.setTime(3, Time.valueOf(normalizeTime(startTime)));
            ps.setTime(4, Time.valueOf(normalizeTime(endTime)));
            ps.setString(5,location);
            ps.setString(6,notes);
            ps.setString(7,status);
            ps.setInt(8,id);
            return ps.executeUpdate()>0;
        } catch(Exception e){ System.out.println(e); return false; }
    }

    /** Deletes a schedule entry by id. */
    public boolean deleteSchedule(int id){
        try (Connection c=DBConnection.getConnection()){
            if (c == null) return false;
            PreparedStatement ps=c.prepareStatement("DELETE FROM schedules WHERE scheduleId=?");
            ps.setInt(1,id);
            return ps.executeUpdate()>0;
        } catch(Exception e){ System.out.println(e); return false; }
    }

    /** Returns every schedule entry joined with its tour name. */
    public List<Object[]> getAll(){
        List<Object[]> list=new ArrayList<>();
        try (Connection c=DBConnection.getConnection()){
            if (c == null) return list;
            ResultSet rs=c.createStatement().executeQuery(
              "SELECT s.scheduleId, s.tourId, t.name, s.tourDate, s.startTime, s.endTime, s.location, s.notes, s.status " +
              "FROM schedules s LEFT JOIN tours t ON s.tourId = t.tourId " +
              "ORDER BY s.tourDate ASC, s.startTime ASC");
            while(rs.next()) list.add(rowFrom(rs));
        } catch(Exception e){ System.out.println(e); }
        return list;
    }

    /** Filters schedule entries by tour name, date or status. */
    public List<Object[]> search(String q){
        List<Object[]> list=new ArrayList<>();
        try (Connection c=DBConnection.getConnection()){
            if (c == null) return list;
            PreparedStatement ps=c.prepareStatement(
              "SELECT s.scheduleId, s.tourId, t.name, s.tourDate, s.startTime, s.endTime, s.location, s.notes, s.status " +
              "FROM schedules s LEFT JOIN tours t ON s.tourId = t.tourId " +
              "WHERE t.name LIKE ? OR s.location LIKE ? OR s.status LIKE ? " +
              "ORDER BY s.tourDate ASC, s.startTime ASC");
            String like = "%" + q + "%";
            ps.setString(1, like); ps.setString(2, like); ps.setString(3, like);
            ResultSet rs=ps.executeQuery();
            while(rs.next()) list.add(rowFrom(rs));
        } catch(Exception e){ System.out.println(e); }
        return list;
    }

    private Object[] rowFrom(ResultSet rs) throws SQLException {
        return new Object[]{
            rs.getInt(1), rs.getInt(2), rs.getString(3),
            rs.getDate(4)==null?"":rs.getDate(4).toString(),
            rs.getTime(5)==null?"":rs.getTime(5).toString().substring(0,5),
            rs.getTime(6)==null?"":rs.getTime(6).toString().substring(0,5),
            rs.getString(7), rs.getString(8), rs.getString(9)
        };
    }

    /** Accepts "HH:mm" from the form and turns it into JDBC's "HH:mm:ss". */
    private String normalizeTime(String t){
        t = t==null? "" : t.trim();
        return t.length()==5 ? t+":00" : t;
    }
}
