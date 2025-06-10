package edu.uoc.uoctron.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class Result {
    private LocalDateTime time;
    private double generatedMW;
    private double expectedDemandMW;
    private double averageStability;
    private Map<String, Double> generatedByTypeMW;

    public Result() {

    }

    public Result(LocalDateTime time, double generatedMW, double expectedDemandMW,
                  double averageStability, Map<String, Double> generatedByTypeMW) {
        this.time = time;
        this.generatedMW = generatedMW;
        this.expectedDemandMW = expectedDemandMW;
        this.averageStability = averageStability;
        this.generatedByTypeMW = new HashMap<>(generatedByTypeMW);
    }

    public LocalDateTime getTime() {
        return time; }
    public double getGeneratedMW() {
        return generatedMW; }
    public double getExpectedDemandMW() {
        return expectedDemandMW; }
    public double getAverageStability() {
        return averageStability; }
    public Map<String, Double> getGeneratedByTypeMW() {
        return generatedByTypeMW; }

}
