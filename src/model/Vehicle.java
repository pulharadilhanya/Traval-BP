package model;

/**
 * Vehicle — a transport resource (car, van or bus) managed by a Service
 * Provider, storing its type, plate number, seat capacity and
 * availability status.
 */
public class Vehicle {
    private int vehicleId, seats;
    private String type, plateNo, status;

    public Vehicle(){}
    public Vehicle(int id, String t, String p, int s, String st){
        this.vehicleId=id; this.type=t; this.plateNo=p; this.seats=s; this.status=st;
    }
    public int getVehicleId(){return vehicleId;}
    public String getType(){return type;}
    public String getPlateNo(){return plateNo;}
    public int getSeats(){return seats;}
    public String getStatus(){return status;}

    public void updateVehicle(String type, String plateNo, int seats, String status){
        this.type = type;
        this.plateNo = plateNo;
        this.seats = seats;
        this.status = status;
    }
    public boolean checkAvailability(){ return "Available".equalsIgnoreCase(status); }
}
