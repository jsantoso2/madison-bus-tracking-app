package datamodel;

public class Arrival {
    /** A Flink POJO must have public fields, or getters and setters */
    public String delay;
    public String time;
    public String uncertainty;

    /** A Flink POJO must have a no-args default constructor */
    public Arrival() {}

    public Arrival(String delay, String time, String uncertainty){
        this.delay = delay;
        this.time = time;
        this.uncertainty = uncertainty;
    }
    
}
