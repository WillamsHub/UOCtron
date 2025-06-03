package edu.uoc.uoctron.model;

import java.time.LocalTime;

public class NuclearPlant extends PowerPlants{

    static final LocalTime INIT_OPERATION_HOUR = LocalTime.of(0,0);
    static final LocalTime FINISH_OPERATION_HOUR = LocalTime.of(23,59);
    static final LocalTime RESTART_TIME = LocalTime.of(23,59);
    static final double STABILITY = 1.0;
    String image = "nuclear.png";

    public NuclearPlant(String type, String name, double latitude, double longitude, String city, double maxCapacityMW, double efficiency){
        super(type, name, latitude, longitude, city, maxCapacityMW, efficiency);
    }

    @Override
    public String toString() {
        return "HOLA";
    }
}
