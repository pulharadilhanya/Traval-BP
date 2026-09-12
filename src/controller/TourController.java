package controller;

import database.DBConnection;

import model.Tour;
import java.sql.*;
import java.util.*;

/**
 * TourController — the Controller (MVC) for Tour records: handles all
 * database CRUD operations for tours and hands back plain Tour model
 * objects to the View layer (Admin dashboard's Manage Tours screen).
 */
public class TourController {
    /** Inserts a new tour record. */
    public boolean addTour(String n,String d,String desc,int dur,float pr,String st){
        try (Connection c=DBConnection.getConnection()){
            PreparedStatement ps=c.prepareStatement(
              "INSERT INTO tours(name,destination,description,durationDays,price,status) VALUES(?,?,?,?,?,?)");
            ps.setString(1,n);ps.setString(2,d);ps.setString(3,desc);
            ps.setInt(4,dur);ps.setFloat(5,pr);ps.setString(6,st);
            return ps.executeUpdate()>0;
        } catch(Exception e){System.out.println(e);return false;}
    }
    /** Updates an existing tour record by id. */
    public boolean updateTour(int id,String n,String d,String desc,int dur,float pr,String st){
        try (Connection c=DBConnection.getConnection()){
            PreparedStatement ps=c.prepareStatement(
              "UPDATE tours SET name=?,destination=?,description=?,durationDays=?,price=?,status=? WHERE tourId=?");
            ps.setString(1,n);ps.setString(2,d);ps.setString(3,desc);
            ps.setInt(4,dur);ps.setFloat(5,pr);ps.setString(6,st);ps.setInt(7,id);
            return ps.executeUpdate()>0;
        } catch(Exception e){return false;}
    }
    /** Deletes a tour record by id. */
    public boolean deleteTour(int id){
        try (Connection c=DBConnection.getConnection()){
            PreparedStatement ps=c.prepareStatement("DELETE FROM tours WHERE tourId=?");
            ps.setInt(1,id); return ps.executeUpdate()>0;
        } catch(Exception e){return false;}
    }
    /** Returns every tour record in the database. */
    public List<Tour> getAllTours(){
        List<Tour> list = new ArrayList<>();
        try (Connection c=DBConnection.getConnection()){
            ResultSet rs=c.createStatement().executeQuery("SELECT * FROM tours");
            while(rs.next()){
                list.add(new Tour(rs.getInt("tourId"),rs.getString("name"),rs.getString("destination"),
                  rs.getString("description"),rs.getInt("durationDays"),rs.getFloat("price"),rs.getString("status")));
            }
        } catch(Exception e){System.out.println(e);}
        return list;
    }
    /** Updates just the status of a tour. */
    public boolean updateStatus(int id,String status){
        try (Connection c=DBConnection.getConnection()){
            PreparedStatement ps=c.prepareStatement("UPDATE tours SET status=? WHERE tourId=?");
            ps.setString(1,status);ps.setInt(2,id);
            return ps.executeUpdate()>0;
        } catch(Exception e){return false;}
    }

    /** Matches the search boxes on the Tours tabs against name, destination or status. */
    public List<Tour> searchTours(String q){
        List<Tour> list = new ArrayList<>();
        try (Connection c=DBConnection.getConnection()){
            PreparedStatement ps=c.prepareStatement(
              "SELECT * FROM tours WHERE name LIKE ? OR destination LIKE ? OR status LIKE ?");
            String like = "%" + q + "%";
            ps.setString(1, like); ps.setString(2, like); ps.setString(3, like);
            ResultSet rs=ps.executeQuery();
            while(rs.next()){
                list.add(new Tour(rs.getInt("tourId"),rs.getString("name"),rs.getString("destination"),
                  rs.getString("description"),rs.getInt("durationDays"),rs.getFloat("price"),rs.getString("status")));
            }
        } catch(Exception e){System.out.println(e);}
        return list;
    }
}
