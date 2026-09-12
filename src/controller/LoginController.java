package controller;

import model.*;

/**
 * LoginController — the single entry point the Login view talks to.
 * Authenticates credentials via UserController, then acts as a factory
 * that hands back the correct User subclass (Admin / TourManager /
 * TourGuide / ServiceProvider) for the logged-in role. Callers can then
 * use polymorphism, e.g. loggedInUser.getHomeTitle() or
 * loggedInUser.getPermissions(), without an if/else on role strings.
 */
public class LoginController {
    private final UserController userController = new UserController();

    /** Returns the logged-in user as the correct role subclass, or null if invalid. */
    public User login(String username, String password) {
        String role = userController.login(username, password);
        if (role == null) return null;

        User u = userController.getUserByUsername(username);
        if (u == null) return null;

        return switch (role) {
            case "Admin" -> new Admin(u.getUserId(), u.getUsername(), u.getPassword(), u.getEmail(), u.getContact());
            case "TourManager" -> new TourManager(u.getUserId(), u.getUsername(), u.getPassword(), u.getEmail(), u.getContact());
            case "TourGuide" -> new TourGuide(u.getUserId(), u.getUsername(), u.getPassword(), u.getEmail(), u.getContact());
            case "ServiceProvider" -> new ServiceProvider(u.getUserId(), u.getUsername(), u.getPassword(), u.getEmail(), u.getContact());
            default -> u; // unknown role — fall back to the plain User
        };
    }
}
