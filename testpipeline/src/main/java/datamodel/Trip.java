package datamodel;

public class Trip {
    /** A Flink POJO must have public fields, or getters and setters */
    public String tripId;
    public String startTime;
    public String startDate;
    public String routeId;
    public String scheduleRelationship;
    public String directionId;

    /** A Flink POJO must have a no-args default constructor */
    public Trip() {}

    public Trip(String tripId, String startTime, String startDate, String routeId, String scheduleRelationship, String directionId){
        this.tripId = tripId;
        this.startTime = startTime;
        this.startDate = startDate;
        this.routeId = routeId;
        this.scheduleRelationship = scheduleRelationship;
        this.directionId = directionId;
    }
}
