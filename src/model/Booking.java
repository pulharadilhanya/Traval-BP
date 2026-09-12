package model;

/**
 * Booking — a customer's reservation for a Tour, recording the number of
 * tourists, the booking date, the total amount charged, and its status
 * (Pending, Confirmed or Cancelled).
 */
public class Booking {
    private int bookingId, tourId, tourists;
    private String customerName, bookingDate, status;
    private float totalAmount;

    public Booking(){}
    public Booking(int id, int tid, String cn, int t, String bd, float ta, String s){
        this.bookingId=id; this.tourId=tid; this.customerName=cn;
        this.tourists=t; this.bookingDate=bd; this.totalAmount=ta; this.status=s;
    }
    public int getBookingId(){return bookingId;}
    public int getTourId(){return tourId;}
    public String getCustomerName(){return customerName;}
    public int getTourists(){return tourists;}
    public String getBookingDate(){return bookingDate;}
    public float getTotalAmount(){return totalAmount;}
    public String getStatus(){return status;}

    public void createBooking(){ this.status = "Pending"; }
    public String viewBooking(){
        return "Booking #"+bookingId+" - "+customerName+" | Tour:"+tourId+
               " | Tourists:"+tourists+" | Date:"+bookingDate+
               " | Total:"+totalAmount+" | Status:"+status;
    }
    public void updateBooking(int tourists, float totalAmount, String status){
        this.tourists = tourists;
        this.totalAmount = totalAmount;
        this.status = status;
    }
    public void cancelBooking(){ this.status = "Cancelled"; }
}
