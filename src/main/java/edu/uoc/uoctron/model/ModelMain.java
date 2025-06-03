package edu.uoc.uoctron.model;

import java.time.LocalTime;
import java.util.LinkedList;

public class ModelMain {

    LinkedList<PowerPlants> plants = new LinkedList<>();
    LinkedList<DemandMinute> demandMinute = new LinkedList<>();
    LinkedList<SimulaitonResults> simulationResults = new LinkedList<>();

    public ModelMain(){

    }
    public void addPlant(String type, String name, double latitude, double longitude, String city, double maxCapacityMW, double efficiency){
        switch (type){
            case "NUCLEAR":
                plants.add(new NuclearPlant(type, name, latitude, longitude, city, maxCapacityMW, efficiency));
            case "SOLAR":
                plants.add(new SolarPlant(type, name, latitude, longitude, city, maxCapacityMW, efficiency));
            case "WIND":
                plants.add(new WindPlant(type, name, latitude, longitude, city, maxCapacityMW, efficiency));
            case "HYDRO":
                plants.add(new HydroPlant(type, name, latitude, longitude, city, maxCapacityMW, efficiency));
            case "GEOTHERMAL":
                plants.add(new GeothermalPlant(type, name, latitude, longitude, city, maxCapacityMW, efficiency));
            case "COMBINED_CYCLE":
                plants.add(new CombinedCyclePlant(type, name, latitude, longitude, city, maxCapacityMW, efficiency));
            case "COAL":
                plants.add(new CoalPlant(type, name, latitude, longitude, city, maxCapacityMW, efficiency));
            case "BIOMASS":
                plants.add(new BiomassPlant(type, name, latitude, longitude, city, maxCapacityMW, efficiency));
            case "FUEL_GAS":
                plants.add(new FuelGasPlant(type, name, latitude, longitude, city, maxCapacityMW, efficiency));

            default:
        }

    }
    public void addMinuteDemand(LocalTime time, double demand){

        demandMinute.add(new DemandMinute(time, demand));
    }

    public LinkedList<PowerPlants> getPlants() {
        return plants;
    }

    public void setPlants(LinkedList<PowerPlants> plants) {
        this.plants = plants;
    }

    public LinkedList<DemandMinute> getDemandMinute() {
        return demandMinute;
    }

    public void setDemandMinute(LinkedList<DemandMinute> demandMinute) {
        this.demandMinute = demandMinute;
    }

    public LinkedList<SimulaitonResults> getSimulationResults() {
        return simulationResults;
    }

    public void setSimulationResults(LinkedList<SimulaitonResults> simulationResults) {
        this.simulationResults = simulationResults;
    }
}
