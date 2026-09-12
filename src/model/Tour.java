package model;

/**
 * Tour — a travel package record: destination, description, duration,
 * price and status. TourPackage extends this class to represent the
 * same data from the Tour Manager's point of view (inheritance).
 */
public class Tour {
    private int tourId;
    private String name, destination, description;
    private int durationDays;
    private float price;
    private String status;

    public Tour(){}
    public Tour(int id, String n, String d, String desc, int dur, float pr, String st){
        this.tourId=id; this.name=n; this.destination=d; this.description=desc;
        this.durationDays=dur; this.price=pr; this.status=st;
    }
    public int getTourId(){return tourId;}
    public String getName(){return name;}
    public String getDestination(){return destination;}
    public String getDescription(){return description;}
    public int getDurationDays(){return durationDays;}
    public float getPrice(){return price;}
    public String getStatus(){return status;}
}
