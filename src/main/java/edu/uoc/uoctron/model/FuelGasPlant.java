package edu.uoc.uoctron.model;

import java.time.LocalTime;

public class FuelGasPlant extends PowerPlants{

    static final LocalTime INIT_OPERATION_HOUR = LocalTime.of(0,0);
    static final LocalTime FINISH_OPERATION_HOUR = LocalTime.of(23,59);
    static final LocalTime RESTART_TIME = LocalTime.of(4,0);
    static final double STABILITY = 0.6;
    static final String ELEMENT_BURNED = "Gasoline";
    String icon = "fuel_gas.png";
    double maxCapacityMW;

    public FuelGasPlant(String type, String name, double latitude, double longitude, String city, double maxCapacityMW, double efficiency) {
        super(type, name, latitude, longitude, city, maxCapacityMW, efficiency);
    }

    public String getIcon() {
        return icon;
    }
    public LocalTime getInitOperationHour() {
        return INIT_OPERATION_HOUR;
    }
    public LocalTime getFinishOperationHour(){
        return FINISH_OPERATION_HOUR;
    }
    public LocalTime getRestartTime(){
        return RESTART_TIME;
    }
    public double getStability(){
        return STABILITY;
    }
    public String getElementBurned(){
        return ELEMENT_BURNED;
    }
    public void setMaxCapacityMW(double maxCapacityMW){
        this.maxCapacityMW = maxCapacityMW;
    }

    public double getGeneratedMW() {
        return maxCapacityMW;
    }

    @Override
    public String toString() {
        return "{\n\"type\": " + getType() + ",\n" +
                "\"latitude\": " + getLatitude() + ",\n" +
                "\"longitude\": " + getLongitude() + ",\n" +
                "\"icon\": " + getIcon() + ",\n" +
                "}\n{" +
                "\"init_operation_hour\": " + getInitOperationHour() + ",\n" +
                "\"finish_operation_hour\": " + getFinishOperationHour() + ",\n" +
                "\"restart_time\": " + getRestartTime() + ",\n" +
                "\"stability\": " + getStability() + ",\n" +
                "\"element_burned\": " + getElementBurned() + "\n}";
    }
}
