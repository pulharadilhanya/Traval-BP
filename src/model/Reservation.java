package model;

/**
 * Reservation — a confirmed (or pending) booking of a specific Hotel or
 * Vehicle for a customer, managed by the Hotel/Transport Provider.
 */
public class Reservation {
    private int reservationId;
    private String serviceType;   // "Hotel" or "Vehicle"
    private int refId;            // hotelId or vehicleId depending on serviceType
    private String customerName;
    private String reservationDate;
    private String status;        // Pending, Confirmed, Cancelled

    public Reservation() {}

    public Reservation(int reservationId, String serviceType, int refId, String customerName,
                        String reservationDate, String status) {
        this.reservationId = reservationId;
        this.serviceType = serviceType;
        this.refId = refId;
        this.customerName = customerName;
        this.reservationDate = reservationDate;
        this.status = status;
    }

    public int getReservationId() { return reservationId; }
    public String getServiceType() { return serviceType; }
    public int getRefId() { return refId; }
    public String getCustomerName() { return customerName; }
    public String getReservationDate() { return reservationDate; }
    public String getStatus() { return status; }

    public void setReservationId(int reservationId) { this.reservationId = reservationId; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public void setRefId(int refId) { this.refId = refId; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public void setReservationDate(String reservationDate) { this.reservationDate = reservationDate; }
    public void setStatus(String status) { this.status = status; }

    public void createReservation(){ this.status = "Pending"; }
    public void confirmReservation(){ this.status = "Confirmed"; }
    public void cancelReservation(){ this.status = "Cancelled"; }
}
