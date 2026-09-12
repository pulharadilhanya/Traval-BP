package controller;

import database.DBConnection;

import java.sql.*;
import java.util.*;

/**
 * BookingController — the Controller (MVC) for tour Bookings: creates new
 * bookings, updates their approval status, and joins against the tours
 * table to give the Admin dashboard a display-ready list.
 */
public class BookingController {
    /** Creates a new booking with status "Pending". */
    public boolean addBooking(int tourId,String customer,int tourists,float total){
        try (Connection c=DBConnection.getConnection()){
            if (c == null) return false;
            PreparedStatement ps=c.prepareStatement(
              "INSERT INTO bookings(tourId,customerName,tourists,bookingDate,totalAmount,status) VALUES(?,?,?,?,?,'Pending')");
            ps.setInt(1,tourId);ps.setString(2,customer);ps.setInt(3,tourists);
            ps.setString(4, java.time.LocalDate.now().toString());
            ps.setFloat(5,total);
            return ps.executeUpdate()>0;
        } catch(Exception e){System.out.println(e);return false;}
    }
    /** Updates a booking's approval status. */
    public boolean updateStatus(int id,String status){
        try (Connection c=DBConnection.getConnection()){
            PreparedStatement ps=c.prepareStatement("UPDATE bookings SET status=? WHERE bookingId=?");
            ps.setString(1,status);ps.setInt(2,id);
            return ps.executeUpdate()>0;
        } catch(Exception e){return false;}
    }
    /** Returns every booking joined with its tour name, newest first. */
    public List<Object[]> getAll(){
        List<Object[]> list=new ArrayList<>();
        try (Connection c=DBConnection.getConnection()){
            ResultSet rs=c.createStatement().executeQuery(
              "SELECT b.bookingId,b.customerName,t.name,b.tourists,b.bookingDate,b.totalAmount,b.status " +
              "FROM bookings b LEFT JOIN tours t ON b.tourId=t.tourId ORDER BY b.bookingId DESC");
            while(rs.next()){
                list.add(new Object[]{rs.getInt(1),rs.getString(2),rs.getString(3),
                  rs.getInt(4),rs.getString(5),"Rs. "+rs.getFloat(6),rs.getString(7)});
            }
        } catch(Exception e){System.out.println(e);}
        return list;
    }
    /** Matches the search box on the Approve Bookings screen against customer name, tour name or status. */
    public List<Object[]> search(String q){
        List<Object[]> list=new ArrayList<>();
        try (Connection c=DBConnection.getConnection()){
            PreparedStatement ps=c.prepareStatement(
              "SELECT b.bookingId,b.customerName,t.name,b.tourists,b.bookingDate,b.totalAmount,b.status " +
              "FROM bookings b LEFT JOIN tours t ON b.tourId=t.tourId " +
              "WHERE b.customerName LIKE ? OR t.name LIKE ? OR b.status LIKE ? ORDER BY b.bookingId DESC");
            String like = "%"+q+"%";
            ps.setString(1,like); ps.setString(2,like); ps.setString(3,like);
            ResultSet rs=ps.executeQuery();
            while(rs.next()){
                list.add(new Object[]{rs.getInt(1),rs.getString(2),rs.getString(3),
                  rs.getInt(4),rs.getString(5),"Rs. "+rs.getFloat(6),rs.getString(7)});
            }
        } catch(Exception e){System.out.println(e);}
        return list;
    }
}
