package model;

/**
 * Hotel — a hotel resource managed by a Service Provider, storing its
 * name, location, room count, price per night and availability status.
 */
public class Hotel {
    private int hotelId, rooms;
    private String name, location;
    private float pricePerNight;
    private String status;

    public Hotel(){}
    public Hotel(int id, String n, String loc, int r, float p, String s){
        this.hotelId=id; this.name=n; this.location=loc; this.rooms=r; this.pricePerNight=p; this.status=s;
    }
    public int getHotelId(){return hotelId;}
    public String getName(){return name;}
    public String getLocation(){return location;}
    public int getRooms(){return rooms;}
    public float getPricePerNight(){return pricePerNight;}
    public String getStatus(){return status;}

    public boolean checkAvailability(){ return "Available".equalsIgnoreCase(status) && rooms > 0; }
    public void updateRoomAvailability(int rooms, String status){
        this.rooms = rooms;
        this.status = status;
    }
}
