package edu.uoc.uoctron.model;

import edu.uoc.uoctron.exception.ModelMainException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.LinkedList;

public class ModelMain {

    LinkedList<PowerPlants> plants;
    LinkedList<DemandMinute> demandMinute;
    SimulationResults simulationResults;



    public ModelMain(){

        plants = new LinkedList<>();
        demandMinute = new LinkedList<>();
        simulationResults = new SimulationResults(LocalDateTime.now(), plants, demandMinute);

    }
    public void addPlant(String type, String name, double latitude, double longitude, String city, double maxCapacityMW, double efficiency) throws ModelMainException{
        switch (type){
            case "NUCLEAR":
                plants.add(new NuclearPlant(type, name, latitude, longitude, city, maxCapacityMW, efficiency));
                break;
            case "SOLAR":
                plants.add(new SolarPlant(type, name, latitude, longitude, city, maxCapacityMW, efficiency));
                break;
            case "WIND":
                plants.add(new WindPlant(type, name, latitude, longitude, city, maxCapacityMW, efficiency));
                break;
            case "HYDRO":
                plants.add(new HydroPlant(type, name, latitude, longitude, city, maxCapacityMW, efficiency));
                break;
            case "GEOTHERMAL":
                plants.add(new GeothermalPlant(type, name, latitude, longitude, city, maxCapacityMW, efficiency));
                break;
            case "COMBINED_CYCLE":
                plants.add(new CombinedCyclePlant(type, name, latitude, longitude, city, maxCapacityMW, efficiency));
                break;
            case "COAL":
                plants.add(new CoalPlant(type, name, latitude, longitude, city, maxCapacityMW, efficiency));
                break;
            case "BIOMASS":
                plants.add(new BiomassPlant(type, name, latitude, longitude, city, maxCapacityMW, efficiency));
                break;
            case "FUEL_GAS":
                plants.add(new FuelGasPlant(type, name, latitude, longitude, city, maxCapacityMW, efficiency));
                break;

            default:
                throw new ModelMainException(ModelMainException.ERROR_NULL_PLANT);
        }

    }
    public void addMinuteDemand(LocalTime time, double demand){

        demandMinute.add(new DemandMinute(time, demand));
    }

    public LinkedList<PowerPlants> getPlants() {
        return plants;
    }



    public LinkedList<DemandMinute> getDemandMinute() {
        return demandMinute;
    }



    public JSONArray getSimulationResults() {

        return new JSONArray(simulationResults.getResults());
    }


    public void runBlackoutSimulation(LocalDateTime blackoutStart){

        simulationResults = new SimulationResults(blackoutStart,  getPlants(), getDemandMinute());
        simulationResults.simulate();

    }


}
