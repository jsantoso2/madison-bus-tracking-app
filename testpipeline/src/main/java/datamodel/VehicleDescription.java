package datamodel;

public class VehicleDescription {
    /** A Flink POJO must have public fields, or getters and setters */
    public String id;
    public String label;

    /** A Flink POJO must have a no-args default constructor */
    public VehicleDescription() {}

    public VehicleDescription(String id, String label){
        this.id = id;
        this.label = label;
    }
    
}
