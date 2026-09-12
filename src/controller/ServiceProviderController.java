package controller;

import database.DBConnection;

import model.Reservation;
import java.sql.*;
import java.util.*;

/**
 * ServiceProviderController — what a Hotel/Transport Provider does:
 * update hotel/vehicle status (delegated to HotelController/VehicleController)
 * and manage reservations against those hotels/vehicles.
 */
public class ServiceProviderController {
    private final HotelController hotelController = new HotelController();
    private final VehicleController vehicleController = new VehicleController();

    /** Updates a hotel's details/status (delegates to HotelController). */
    public boolean updateHotelStatus(int hotelId, String name, String location, int rooms, float price, String status) {
        return hotelController.updateHotel(hotelId, name, location, rooms, price, status);
    }

    /** Updates a vehicle's details/status (delegates to VehicleController). */
    public boolean updateVehicleStatus(int vehicleId, String type, String plate, int seats, String status) {
        return vehicleController.updateVehicle(vehicleId, type, plate, seats, status);
    }

    /** Creates a new reservation against a hotel or vehicle. */
    public boolean addReservation(String serviceType, int refId, String customerName, String status) {
        try (Connection c = DBConnection.getConnection()) {
            if (c == null) return false;
            PreparedStatement ps = c.prepareStatement(
                "INSERT INTO reservations(serviceType,refId,customerName,reservationDate,status) VALUES(?,?,?,?,?)");
            ps.setString(1, serviceType);
            ps.setInt(2, refId);
            ps.setString(3, customerName);
            ps.setString(4, java.time.LocalDate.now().toString());
            ps.setString(5, status);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { System.out.println(e); return false; }
    }

    /** Confirms a reservation — this is the "Confirm reservations" responsibility. */
    public boolean confirmReservation(int reservationId) {
        return updateReservationStatus(reservationId, "Confirmed");
    }

    /** Updates a reservation's status (e.g. Pending -> Confirmed). */
    public boolean updateReservationStatus(int reservationId, String status) {
        try (Connection c = DBConnection.getConnection()) {
            PreparedStatement ps = c.prepareStatement("UPDATE reservations SET status=? WHERE reservationId=?");
            ps.setString(1, status);
            ps.setInt(2, reservationId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { return false; }
    }

    /** Updates a reservation's editable details (type, ref id, customer name); status is unaffected. */
    public boolean updateReservationDetails(int reservationId, String serviceType, int refId, String customerName) {
        try (Connection c = DBConnection.getConnection()) {
            PreparedStatement ps = c.prepareStatement(
                "UPDATE reservations SET serviceType=?, refId=?, customerName=? WHERE reservationId=?");
            ps.setString(1, serviceType);
            ps.setInt(2, refId);
            ps.setString(3, customerName);
            ps.setInt(4, reservationId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { System.out.println(e); return false; }
    }

    /** Deletes a reservation by id. */
    public boolean deleteReservation(int reservationId) {
        try (Connection c = DBConnection.getConnection()) {
            PreparedStatement ps = c.prepareStatement("DELETE FROM reservations WHERE reservationId=?");
            ps.setInt(1, reservationId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { return false; }
    }

    /**
     * Adds a service log entry (Room/Transport/Cleanliness status update).
     * Talks to a new `service_logs` table (logDate, service, status, remarks,
     * updatedBy) that isn't part of the original schema, so it needs to be
     * created once — see the accompanying service_logs_table.sql.
     */
    public boolean addServiceLog(String service, String status, String remarks, String updatedBy) {
        try (Connection c = DBConnection.getConnection()) {
            if (c == null) return false;
            PreparedStatement ps = c.prepareStatement(
                "INSERT INTO service_logs(logDate,service,status,remarks,updatedBy) VALUES(?,?,?,?,?)");
            ps.setString(1, java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a")));
            ps.setString(2, service);
            ps.setString(3, status);
            ps.setString(4, remarks);
            ps.setString(5, updatedBy);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { System.out.println(e); return false; }
    }

    /** Returns every service log entry, newest first, as {logDate,service,status,remarks,updatedBy}. */
    public List<Object[]> getServiceLogs() {
        List<Object[]> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection()) {
            if (c == null) return list;
            ResultSet rs = c.createStatement().executeQuery(
                "SELECT logDate,service,status,remarks,updatedBy FROM service_logs ORDER BY logId DESC");
            while (rs.next()) {
                list.add(new Object[]{rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5)});
            }
        } catch (Exception e) { System.out.println(e); }
        return list;
    }

    /** Matches the search box on the Service Status screen against service, status or remarks. */
    public List<Object[]> searchServiceLogs(String q) {
        List<Object[]> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection()) {
            if (c == null) return list;
            PreparedStatement ps = c.prepareStatement(
                "SELECT logDate,service,status,remarks,updatedBy FROM service_logs " +
                "WHERE service LIKE ? OR status LIKE ? OR remarks LIKE ? ORDER BY logId DESC");
            String like = "%"+q+"%";
            ps.setString(1, like); ps.setString(2, like); ps.setString(3, like);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Object[]{rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5)});
            }
        } catch (Exception e) { System.out.println(e); }
        return list;
    }

    /** Returns every reservation in the database. */
    public List<Reservation> getAllReservations() {
        List<Reservation> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection()) {
            ResultSet rs = c.createStatement().executeQuery(
                "SELECT reservationId,serviceType,refId,customerName,reservationDate,status " +
                "FROM reservations ORDER BY reservationId DESC");
            while (rs.next()) {
                list.add(new Reservation(rs.getInt(1), rs.getString(2), rs.getInt(3),
                        rs.getString(4), rs.getString(5), rs.getString(6)));
            }
        } catch (Exception e) { System.out.println(e); }
        return list;
    }
}
