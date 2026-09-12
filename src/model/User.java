package model;

/**
 * User — the base account/role class for everyone who logs into the
 * system. Admin, TourManager, TourGuide and ServiceProvider all extend
 * this class (inheritance) and override getPermissions()/getHomeTitle()
 * to supply their own role-specific behaviour (polymorphism).
 */
public class User {
    private int userId;
    private String username, password, email, contact, role;

    public User() {}
    public User(int id, String u, String p, String e, String c, String r) {
        this.userId=id; this.username=u; this.password=p; this.email=e; this.contact=c; this.role=r;
    }
    public int getUserId(){return userId;}
    public String getUsername(){return username;}
    public String getPassword(){return password;}
    public String getEmail(){return email;}
    public String getContact(){return contact;}
    public String getRole(){return role;}
    public void setUserId(int i){userId=i;}
    public void setUsername(String s){username=s;}
    public void setPassword(String s){password=s;}
    public void setEmail(String s){email=s;}
    public void setContact(String s){contact=s;}
    public void setRole(String s){role=s;}

    /**
     * Overridden by each role subclass (Admin, TourManager, TourGuide,
     * ServiceProvider) to describe what that role is allowed to do.
     * Demonstrates polymorphism: LoginController hands back the correct
     * subtype, and callers can invoke getPermissions()/getHomeTitle()
     * without knowing which role it is.
     */
    public String[] getPermissions(){
        return new String[]{};
    }

    /** Short label shown as the dashboard title for this role. */
    public String getHomeTitle(){
        return "User Dashboard";
    }
}
