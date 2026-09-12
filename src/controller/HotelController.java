package controller;

import database.DBConnection;

import model.Hotel;
import java.sql.*;
import java.util.*;

/**
 * HotelController — the Controller (MVC) for Hotel records: handles all
 * database CRUD operations for hotels and hands back plain Hotel model
 * objects to the View layer (Tour Manager / Service Provider dashboards).
 */
public class HotelController {
    /** Inserts a new hotel record. */
    public boolean addHotel(String n,String loc,int r,float p,String s){
        try (Connection c=DBConnection.getConnection()){
            PreparedStatement ps=c.prepareStatement(
              "INSERT INTO hotels(name,location,rooms,pricePerNight,status) VALUES(?,?,?,?,?)");
            ps.setString(1,n);ps.setString(2,loc);ps.setInt(3,r);ps.setFloat(4,p);ps.setString(5,s);
            return ps.executeUpdate()>0;
        } catch(Exception e){return false;}
    }
    /** Updates an existing hotel record by id. */
    public boolean updateHotel(int id,String n,String loc,int r,float p,String s){
        try (Connection c=DBConnection.getConnection()){
            PreparedStatement ps=c.prepareStatement(
              "UPDATE hotels SET name=?,location=?,rooms=?,pricePerNight=?,status=? WHERE hotelId=?");
            ps.setString(1,n);ps.setString(2,loc);ps.setInt(3,r);ps.setFloat(4,p);ps.setString(5,s);ps.setInt(6,id);
            return ps.executeUpdate()>0;
        } catch(Exception e){return false;}
    }
    /** Deletes a hotel record by id. */
    public boolean deleteHotel(int id){
        try (Connection c=DBConnection.getConnection()){
            PreparedStatement ps=c.prepareStatement("DELETE FROM hotels WHERE hotelId=?");
            ps.setInt(1,id); return ps.executeUpdate()>0;
        } catch(Exception e){return false;}
    }
    /** Returns every hotel record in the database. */
    public List<Hotel> getAll(){
        List<Hotel> l=new ArrayList<>();
        try (Connection c=DBConnection.getConnection()){
            ResultSet rs=c.createStatement().executeQuery("SELECT * FROM hotels");
            while(rs.next()){
                l.add(new Hotel(rs.getInt("hotelId"),rs.getString("name"),rs.getString("location"),
                  rs.getInt("rooms"),rs.getFloat("pricePerNight"),rs.getString("status")));
            }
        } catch(Exception e){System.out.println(e);}
        return l;
    }
    /** Matches the search box on the Manage Hotels screen against name, location or status. */
    public List<Hotel> searchHotels(String q){
        List<Hotel> l=new ArrayList<>();
        try (Connection c=DBConnection.getConnection()){
            PreparedStatement ps=c.prepareStatement(
              "SELECT * FROM hotels WHERE name LIKE ? OR location LIKE ? OR status LIKE ?");
            String like = "%"+q+"%";
            ps.setString(1,like); ps.setString(2,like); ps.setString(3,like);
            ResultSet rs=ps.executeQuery();
            while(rs.next()){
                l.add(new Hotel(rs.getInt("hotelId"),rs.getString("name"),rs.getString("location"),
                  rs.getInt("rooms"),rs.getFloat("pricePerNight"),rs.getString("status")));
            }
        } catch(Exception e){System.out.println(e);}
        return l;
    }
}
