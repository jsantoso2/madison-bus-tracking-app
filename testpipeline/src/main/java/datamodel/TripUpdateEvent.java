package datamodel;

public class TripUpdateEvent {
    /** A Flink POJO must have public fields, or getters and setters */
    public String id;
    public TripUpdate tripUpdate;

    /** A Flink POJO must have a no-args default constructor */
    public TripUpdateEvent() {}

    public TripUpdateEvent(String id, TripUpdate tripUpdate) {
        this.id = id;
        this.tripUpdate = tripUpdate;
    }
}
