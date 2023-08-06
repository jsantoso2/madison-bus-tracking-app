package datamodel;

public class TripUpdate {
    /** A Flink POJO must have public fields, or getters and setters */
    public Trip trip;
    public StopTimeUpdateElement[] stopTimeUpdate;
    public VehicleDescription vehicle;
    public long timestamp;


    /** A Flink POJO must have a no-args default constructor */
    public TripUpdate() {}

    public TripUpdate(Trip trip, StopTimeUpdateElement[] stopTimeUpdate, VehicleDescription vehicle, long timestamp){
        this.trip = trip;
        this.stopTimeUpdate = stopTimeUpdate;
        this.vehicle = vehicle;
        this.timestamp = timestamp;
    }
}
