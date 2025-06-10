package edu.uoc.uoctron.model;

import java.time.LocalTime;

public abstract class PowerPlants {
    String type;
    String name;
    double latitude;
    double longitude;
    String city;
    double maxCapacityMW;
    double efficiency;
    protected LocalTime initOperationHour;
    protected LocalTime finishOperationHour;

    public PowerPlants (String type, String name, double latitude, double longitude, String city, double maxCapacityMW, double efficiency){

        setType(type);
        setName(name);
        setLatitude(latitude);
        setLongitude(longitude);
        setCity(city);
        setMaxCapacityMW(maxCapacityMW);
        setEfficiency(efficiency);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public double getMaxCapacityMW() {
        return maxCapacityMW;
    }

    public void setMaxCapacityMW(double maxCapacityMW) {
        this.maxCapacityMW = maxCapacityMW;
    }

    public double getEfficiency() {
        return efficiency;
    }

    public void setEfficiency(double efficiency) {
        this.efficiency = efficiency;
    }

    public LocalTime getInitOperationHour() {
        return initOperationHour;
    }

    public void setInitOperationHour(LocalTime initOperationHour) {
        this.initOperationHour = initOperationHour;
    }

    public LocalTime getFinishOperationHour() {
        return finishOperationHour;
    }

    public void setFinishOperationHour(LocalTime finishOperationHour) {
        this.finishOperationHour = finishOperationHour;
    }
}
