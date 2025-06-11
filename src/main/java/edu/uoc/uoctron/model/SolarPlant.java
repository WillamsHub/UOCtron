package edu.uoc.uoctron.model;

import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class SolarPlant extends PowerPlants{

    static final LocalTime INIT_OPERATION_HOUR = LocalTime.of(7,0);
    static final LocalTime FINISH_OPERATION_HOUR = LocalTime.of(18,59);
    static final LocalTime RESTART_TIME = LocalTime.of(0,6);
    static final double STABILITY = 0.1;
    String icon = "solar.png";
    double maxCapacityMW;

    public SolarPlant(String type, String name, double latitude, double longitude, String city, double maxCapacityMW, double efficiency) {
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
        return json.toString(2);
    }

}
