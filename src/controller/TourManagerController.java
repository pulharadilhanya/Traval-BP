package controller;

import model.Tour;
import model.TourPackage;
import java.util.ArrayList;
import java.util.List;

/**
 * TourManagerController — the Tour Manager's own layer over TourController.
 * Adds/updates/removes tour packages and manages their schedule status,
 * returning TourPackage objects (a Tour subtype) so the dashboard code
 * reads naturally in "package" terms.
 */
public class TourManagerController {
    private final TourController tourController = new TourController();

    public boolean addPackage(String name, String destination, String description,
                               int durationDays, float price, String status) {
        return tourController.addTour(name, destination, description, durationDays, price, status);
    }

    public boolean updatePackage(int id, String name, String destination, String description,
                                  int durationDays, float price, String status) {
        return tourController.updateTour(id, name, destination, description, durationDays, price, status);
    }

    /** Deletes a tour package by id. */
    public boolean deletePackage(int id) {
        return tourController.deleteTour(id);
    }

    /** Updates just the schedule/progress status of a package (e.g. Available -> In-Progress). */
    public boolean updateSchedule(int id, String status) {
        return tourController.updateStatus(id, status);
    }

    /** Returns every tour package. */
    public List<TourPackage> getAllPackages() {
        List<TourPackage> list = new ArrayList<>();
        for (Tour t : tourController.getAllTours()) list.add(TourPackage.from(t));
        return list;
    }

    /** Filters tour packages by name, destination or status. */
    public List<TourPackage> searchPackages(String q) {
        List<TourPackage> list = new ArrayList<>();
        for (Tour t : tourController.searchTours(q)) list.add(TourPackage.from(t));
        return list;
    }
}
