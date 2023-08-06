package datamodel;

public class StopTimeUpdateElement {
    /** A Flink POJO must have public fields, or getters and setters */
    public String stopSequence;
    public Departure departure;
    public Arrival arrival;
    public String stopId;
    public String scheduleRelationship;


    /** A Flink POJO must have a no-args default constructor */
    public StopTimeUpdateElement() {}

    public StopTimeUpdateElement(String stopSequence, Departure departure, Arrival arrival, String stopId, String scheduleRelationship){
        this.stopSequence = stopSequence;
        this.departure = departure;
        this.arrival = arrival;
        this.stopId = stopId;
        this.scheduleRelationship = scheduleRelationship;
    }
    
}
