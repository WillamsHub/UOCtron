package edu.uoc.uoctron.model;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class DemandMinute {

    LocalTime time;
    double demand;
    public DemandMinute (LocalTime time, double demand){
        setTime(time);
        setDemand(demand);
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public double getDemand() {
        return demand;
    }

    public void setDemand(double demand) {
        this.demand = demand;
    }
}
