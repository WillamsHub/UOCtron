package edu.uoc.uoctron.model;

import edu.uoc.uoctron.utils.Utils;
import org.json.JSONObject;

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
        super.setRestartTime(RESTART_TIME);
        super.setStability(STABILITY);
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

    public double getGeneratedMW() {
        return maxCapacityMW;
    }

    @Override
    public String toString() {
        JSONObject json = super.toJSONObject();
        json.put("icon", getIcon());
        json.put("init_operation_hour", getInitOperationHour());
        json.put("finish_operation_hour", getFinishOperationHour());
        json.put("restart_time", getRestartTime());
        json.put("stability", getStability());
        json.put("element_burned", getElementBurned());
        return json.toString(2);
    }
}
