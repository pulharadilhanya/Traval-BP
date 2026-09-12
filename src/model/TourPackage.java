package model;

/**
 * TourPackage — the travel package a Tour Manager builds (destination,
 * itinerary, price, duration). The project's "tours" table already
 * stores exactly this data, so TourPackage extends Tour and simply
 * adds the Tour Manager's own name for clarity when reading code that
 * talks about "packages" rather than raw "tours" — no duplicate table
 * needed, and every Tour is-a TourPackage-compatible object.
 */
public class TourPackage extends Tour {

    public TourPackage() { super(); }

    public TourPackage(int id, String name, String destination, String description,
                        int durationDays, float price, String status) {
        super(id, name, destination, description, durationDays, price, status);
    }

    /** Convenience factory so controllers can hand back a TourPackage view of a Tour row. */
    public static TourPackage from(Tour t) {
        return new TourPackage(t.getTourId(), t.getName(), t.getDestination(), t.getDescription(),
                t.getDurationDays(), t.getPrice(), t.getStatus());
    }
}
