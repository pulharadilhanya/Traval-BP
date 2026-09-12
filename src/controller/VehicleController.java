package controller;

import database.DBConnection;

import model.Vehicle;
import java.sql.*;
import java.util.*;

/**
 * VehicleController — the Controller (MVC) for Vehicle records: handles
 * all database CRUD operations for vehicles and hands back plain Vehicle
 * model objects to the View layer (Tour Manager / Service Provider
 * dashboards).
 */
public class VehicleController {
    /** Inserts a new vehicle record. */
    public boolean addVehicle(String t,String p,int s,String st){
        try (Connection c=DBConnection.getConnection()){
            PreparedStatement ps=c.prepareStatement(
              "INSERT INTO vehicles(type,plateNo,seats,status) VALUES(?,?,?,?)");
            ps.setString(1,t);ps.setString(2,p);ps.setInt(3,s);ps.setString(4,st);
            return ps.executeUpdate()>0;
        } catch(Exception e){return false;}
    }
    /** Updates an existing vehicle record by id. */
    public boolean updateVehicle(int id,String t,String p,int s,String st){
        try (Connection c=DBConnection.getConnection()){
            PreparedStatement ps=c.prepareStatement(
              "UPDATE vehicles SET type=?,plateNo=?,seats=?,status=? WHERE vehicleId=?");
            ps.setString(1,t);ps.setString(2,p);ps.setInt(3,s);ps.setString(4,st);ps.setInt(5,id);
            return ps.executeUpdate()>0;
        } catch(Exception e){return false;}
    }
    /** Deletes a vehicle record by id. */
    public boolean deleteVehicle(int id){
        try (Connection c=DBConnection.getConnection()){
            PreparedStatement ps=c.prepareStatement("DELETE FROM vehicles WHERE vehicleId=?");
            ps.setInt(1,id); return ps.executeUpdate()>0;
        } catch(Exception e){return false;}
    }
    /** Returns every vehicle record in the database. */
    public List<Vehicle> getAll(){
        List<Vehicle> l=new ArrayList<>();
        try (Connection c=DBConnection.getConnection()){
            ResultSet rs=c.createStatement().executeQuery("SELECT * FROM vehicles");
            while(rs.next()){
                l.add(new Vehicle(rs.getInt("vehicleId"),rs.getString("type"),
                  rs.getString("plateNo"),rs.getInt("seats"),rs.getString("status")));
            }
        } catch(Exception e){System.out.println(e);}
        return l;
    }
    /** Matches the search box on the Manage Vehicles screen against type, plate number or status. */
    public List<Vehicle> searchVehicles(String q){
        List<Vehicle> l=new ArrayList<>();
        try (Connection c=DBConnection.getConnection()){
            PreparedStatement ps=c.prepareStatement(
              "SELECT * FROM vehicles WHERE type LIKE ? OR plateNo LIKE ? OR status LIKE ?");
            String like = "%"+q+"%";
            ps.setString(1,like); ps.setString(2,like); ps.setString(3,like);
            ResultSet rs=ps.executeQuery();
            while(rs.next()){
                l.add(new Vehicle(rs.getInt("vehicleId"),rs.getString("type"),
                  rs.getString("plateNo"),rs.getInt("seats"),rs.getString("status")));
            }
        } catch(Exception e){System.out.println(e);}
        return l;
    }
}
