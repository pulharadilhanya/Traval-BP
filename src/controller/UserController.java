package controller;

import database.DBConnection;

import model.User;
import java.sql.*;
import java.util.*;

/**
 * UserController — the Controller (MVC) for User accounts: handles login
 * authentication, and CRUD operations for user records shown on the
 * Admin dashboard's Manage Users screen.
 */
public class UserController {
    public String login(String username, String password){
        try (Connection c = DBConnection.getConnection()) {
            if (c == null) return null;
            PreparedStatement ps = c.prepareStatement("SELECT role, password FROM users WHERE username=?");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                if (PasswordUtil.verifyPassword(password, storedPassword)) {
                    return rs.getString("role");
                }
            }
        } catch (Exception e) { System.out.println(e); }
        return null;
    }

    /** Inserts a new user account with a hashed password. */
    public boolean addUser(String u,String p,String e,String c,String r){
        try (Connection con = DBConnection.getConnection()){
            PreparedStatement ps=con.prepareStatement(
              "INSERT INTO users(username,password,email,contact,role) VALUES(?,?,?,?,?)");
            ps.setString(1,u);ps.setString(2,PasswordUtil.hashPassword(p));ps.setString(3,e);ps.setString(4,c);ps.setString(5,r);
            return ps.executeUpdate()>0;
        } catch(Exception ex){System.out.println(ex);return false;}
    }

    /** Updates a user AND sets a new password (hashed). Use when the admin actually typed a new password. */
    public boolean updateUser(int id,String u,String p,String e,String c,String r){
        try (Connection con = DBConnection.getConnection()){
            PreparedStatement ps=con.prepareStatement(
              "UPDATE users SET username=?,password=?,email=?,contact=?,role=? WHERE userId=?");
            ps.setString(1,u);ps.setString(2,PasswordUtil.hashPassword(p));ps.setString(3,e);ps.setString(4,c);ps.setString(5,r);ps.setInt(6,id);
            return ps.executeUpdate()>0;
        } catch(Exception ex){return false;}
    }

    /** Updates a user WITHOUT touching their password — use when the password field was left blank. */
    public boolean updateUserKeepPassword(int id,String u,String e,String c,String r){
        try (Connection con = DBConnection.getConnection()){
            PreparedStatement ps=con.prepareStatement(
              "UPDATE users SET username=?,email=?,contact=?,role=? WHERE userId=?");
            ps.setString(1,u);ps.setString(2,e);ps.setString(3,c);ps.setString(4,r);ps.setInt(5,id);
            return ps.executeUpdate()>0;
        } catch(Exception ex){return false;}
    }

    /** Deletes a user account by id. */
    public boolean deleteUser(int id){
        try (Connection con = DBConnection.getConnection()){
            PreparedStatement ps=con.prepareStatement("DELETE FROM users WHERE userId=?");
            ps.setInt(1,id);
            return ps.executeUpdate()>0;
        } catch(Exception e){return false;}
    }

    /** Returns every user account as display rows for the Manage Users table. */
    public List<Object[]> getAllUsers(){
        List<Object[]> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection()){
            ResultSet rs = con.createStatement().executeQuery(
              "SELECT userId,username,email,contact,role FROM users");
            while(rs.next()){
                list.add(new Object[]{rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5)});
            }
        } catch(Exception e){System.out.println(e);}
        return list;
    }

    /** Fetches the full user row for a username — used by LoginController to build the
     *  correct role subclass (Admin/TourManager/TourGuide/ServiceProvider) after login. */
    public User getUserByUsername(String username){
        try (Connection c = DBConnection.getConnection()) {
            if (c == null) return null;
            PreparedStatement ps = c.prepareStatement(
              "SELECT userId,username,password,email,contact,role FROM users WHERE username=?");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new User(rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6));
            }
        } catch (Exception e) { System.out.println(e); }
        return null;
    }

    /** Matches the search box on the Admin > Users tab against username, email, contact or role. */
    public List<Object[]> searchUsers(String q){
        List<Object[]> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection()){
            PreparedStatement ps = con.prepareStatement(
              "SELECT userId,username,email,contact,role FROM users " +
              "WHERE username LIKE ? OR email LIKE ? OR contact LIKE ? OR role LIKE ?");
            String like = "%" + q + "%";
            ps.setString(1, like); ps.setString(2, like); ps.setString(3, like); ps.setString(4, like);
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                list.add(new Object[]{rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5)});
            }
        } catch(Exception e){System.out.println(e);}
        return list;
    }
}
