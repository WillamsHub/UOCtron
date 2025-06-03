package edu.uoc.uoctron.model;

public class GeothermalPlant extends PowerPlants{

    public GeothermalPlant(String type, String name, double latitude, double longitude, String city, double maxCapacityMW, double efficiency) {
        super(type, name, latitude, longitude, city, maxCapacityMW, efficiency);
    }
}
