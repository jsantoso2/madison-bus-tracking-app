package datamodel;

public class Position {
    /** A Flink POJO must have public fields, or getters and setters */
    public float latitude;
    public float longitude;
    public float bearing;
    public float speed;

    /** A Flink POJO must have a no-args default constructor */
    public Position() {}

    public Position(float latitude, float longitude, float bearing, float speed){
        this.latitude = latitude;
        this.longitude = longitude;
        this.bearing = bearing;
        this.speed = speed;
    }
}
