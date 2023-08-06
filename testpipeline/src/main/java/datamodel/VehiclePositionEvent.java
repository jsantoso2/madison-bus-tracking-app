package datamodel;

public class VehiclePositionEvent {
    /** A Flink POJO must have public fields, or getters and setters */
    public String id;
    public Vehicle vehicle;

    /** A Flink POJO must have a no-args default constructor */
    public VehiclePositionEvent() {}

    public VehiclePositionEvent(String id, Vehicle vehicle) {
        this.id = id;
        this.vehicle = vehicle;
    }

    // @Override
    // public String toString() {
    //     return "{id: " + this.id + " " + 
    //             "vehicle: { " +  
    //                 "trip: { " + 
    //                     "trip_id: " + this.vehicle.trip.tripId + ", " +
    //                     "start_date: " + this.vehicle.trip.startDate + ", " +
    //                     "route_id: " + this.vehicle.trip.routeId + 
    //                 "}, " +
    //                 "position: { " + 
    //                     "latitude: " + this.vehicle.position.latitude + ", " +
    //                     "longitude: " + this.vehicle.position.longitude + ", " +
    //                     "bearing: " + this.vehicle.position.bearing + " " + 
    //                     "speed: " + this.vehicle.position.speed + 
    //                 "}, " + 
    //                 "timestamp: " + this.vehicle.timestamp + ", " +
    //                 "vehicle: { " + 
    //                     "id: " + this.vehicle.vehicle.id + ", " +
    //                     "label: " + this.vehicle.vehicle.label + 
    //                 "}, " + 
    //                 "occupancyStatus: " + this.vehicle.occupancyStatus + ", " + 
    //                 "occupancyPercentage: " + this.vehicle.occupancyPercentage  + 
    //             "}";
    // }

}
