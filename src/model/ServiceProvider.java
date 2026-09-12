package model;

/**
 * ServiceProvider — a Hotel or Transport provider: updates room
 * availability, manages vehicle details, and confirms reservations.
 */
public class ServiceProvider extends User {

    /** "Hotel" or "Transport" — which kind of service this provider offers. */
    private String serviceType;

    public ServiceProvider() { super(); setRole("ServiceProvider"); }

    public ServiceProvider(int id, String username, String password, String email, String contact) {
        super(id, username, password, email, contact, "ServiceProvider");
    }

    public ServiceProvider(int id, String username, String password, String email, String contact, String serviceType) {
        this(id, username, password, email, contact);
        this.serviceType = serviceType;
    }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    @Override
    public String[] getPermissions() {
        return new String[]{
            "UPDATE_ROOM_AVAILABILITY", "MANAGE_VEHICLES", "CONFIRM_RESERVATIONS", "UPDATE_SERVICE_STATUS"
        };
    }

    @Override
    public String getHomeTitle() {
        return "Service Provider Dashboard";
    }
}
