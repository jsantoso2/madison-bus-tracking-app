package datamodel;

public class Vehicle {
    /** A Flink POJO must have public fields, or getters and setters */
    public Trip trip;
    public Position position;
    public long timestamp;
    public VehicleDescription vehicle;
    public String occupancyStatus;
    public int occupancyPercentage;

    /** A Flink POJO must have a no-args default constructor */
    public Vehicle() {}

    public Vehicle(Trip trip, Position position, Long timestamp, VehicleDescription vehicle, String occupancyStatus, int occupancyPercentage) {
        this.trip = trip;
        this.position = position;
        this.timestamp = timestamp;
        this.vehicle = vehicle;
        this.occupancyStatus = occupancyStatus;
        this.occupancyPercentage = occupancyPercentage;
    }
}
