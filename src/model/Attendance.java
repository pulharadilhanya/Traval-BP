package model;

/** Attendance — a Tour Guide marks whether a tourist was present on a given tour/date. */
public class Attendance {
    private int attendanceId;
    private int tourId;
    private String touristName;
    private String date;
    private boolean present;

    public Attendance() {}

    public Attendance(int attendanceId, int tourId, String touristName, String date, boolean present) {
        this.attendanceId = attendanceId;
        this.tourId = tourId;
        this.touristName = touristName;
        this.date = date;
        this.present = present;
    }

    public int getAttendanceId() { return attendanceId; }
    public int getTourId() { return tourId; }
    public String getTouristName() { return touristName; }
    public String getDate() { return date; }
    public boolean isPresent() { return present; }

    public void setAttendanceId(int attendanceId) { this.attendanceId = attendanceId; }
    public void setTourId(int tourId) { this.tourId = tourId; }
    public void setTouristName(String touristName) { this.touristName = touristName; }
    public void setDate(String date) { this.date = date; }
    public void setPresent(boolean present) { this.present = present; }
}
