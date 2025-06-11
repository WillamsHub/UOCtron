package edu.uoc.uoctron.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class SimulationResults {

    public LocalDateTime blackoutStart;
    public LinkedList<PowerPlants> powerPlants;
    public LinkedList<DemandMinute> demandMinutes;
    public Result result = new Result();
    private LinkedList<Result> results =new LinkedList<>();
    public SimulationResults(LocalDateTime blackoutStart,
                             LinkedList<PowerPlants> powerPlants,
                             LinkedList<DemandMinute> demandMinutes) {
        setBlackoutStart(blackoutStart);
        setPowerPlants(powerPlants);
        setDemandMinutes(demandMinutes);
    }
    public LocalDateTime getBlackoutStart() {
        return this.blackoutStart;
    }
    private void setBlackoutStart(LocalDateTime blackoutStart) {
        this.blackoutStart = blackoutStart;
    }
    public LinkedList<PowerPlants> getPowerPlants() {
        return this.powerPlants;
    }
    private void setPowerPlants(LinkedList<PowerPlants> powerPlants){
        this.powerPlants = powerPlants;
    }
    public LinkedList<DemandMinute> getDemandMinutes(){
        return this.demandMinutes;
    }
    private void setDemandMinutes(LinkedList<DemandMinute> demandMinutes){
        this.demandMinutes = demandMinutes;
    }

    public LinkedList<Result> getResult() {
        return this.results;
    }

    public void simulate() {
        LocalTime currentTime = LocalTime.of(0,0);

        for (int i=0; i< getDemandMinutes().size(); i++ ){
            DemandMinute demandMinute = getDemandMinutes().get(i);

            int offset = demandMinute.getTime().getMinute();
            currentTime = currentTime.plusMinutes(offset);
            LocalDateTime currentTimePlusOffset = blackoutStart.plusMinutes(offset);

            double expectedDemand = getDemandForMinute(offset);
            List<PowerPlants> availablePlants = getAvailablePlants(offset);

            PlantSelection selection = selectOptimalPlants(availablePlants, expectedDemand,offset, currentTime);

            Result result = new Result(
                    currentTimePlusOffset,
                    selection.totalGenerated,
                    expectedDemand,
                    selection.averageStability,
                    selection.generatedByType
            );

            if (result.getAverageStability() >= 0.7) {

                this.results.add(result);
            }
        }

       /* getDemandMinutes().forEach(
                demandMinute -> {
                    int offset = demandMinute.getTime().getMinute();
                    currentTime = currentTime.plusMinutes(offset);
                    LocalDateTime currentTimePlusOffset = blackoutStart.plusMinutes(offset);

                    double expectedDemand = getDemandForMinute(offset);
                    List<PowerPlants> availablePlants = getAvailablePlants(offset);

                    PlantSelection selection = selectOptimalPlants(availablePlants, expectedDemand,offset, currentTime, currentTimePlusOffset);

                    Result result = new Result(
                            currentTimePlusOffset,
                            selection.totalGenerated,
                            expectedDemand,
                            selection.averageStability,
                            selection.generatedByType
                    );

                    if (result.getAverageStability() >= 0.7) {

                        this.results.add(result);
                    }


                }
        );*/

        /*for (int minuteOffset = 0; minuteOffset < 2160; minuteOffset++) {
            LocalDateTime currentTime = blackoutStart.plusMinutes(minuteOffset);

            double expectedDemand = getDemandForMinute(minuteOffset);

            List<PowerPlants> availablePlants = getAvailablePlants(minuteOffset);

            PlantSelection selection = selectOptimalPlants(availablePlants, expectedDemand, minuteOffset);

            Result result = new Result(
                    currentTime,
                    selection.totalGenerated,
                    expectedDemand,
                    selection.averageStability,
                    selection.generatedByType
            );

            this.results.add(result);
        }*/

    }

    private double getDemandForMinute(int minuteOffset) {
        int demandIndex = minuteOffset % 1440;

        if (demandIndex < demandMinutes.size()) {
            return demandMinutes.get(demandIndex).getDemand();
        }
        return 0.0;
    }

    private List<PowerPlants> getAvailablePlants(int minuteOffset) {
        List<PowerPlants> available = new ArrayList<>();

        for (PowerPlants plant : powerPlants) {
            if (isPlantAvailable(plant, minuteOffset)) {
                available.add(plant);
            }
        }

        return available;
    }

    private boolean isPlantAvailable(PowerPlants plant, int minuteOffset) {
        int restartTime = getRestartTime(plant);
        return minuteOffset >= restartTime;
    }

    private int getRestartTime(PowerPlants plant) {
        return switch (plant.getClass().getSimpleName()) {
            case "SolarPlant", "WindPlant" -> 6; // 6 minutos
            case "GeothermalPlant", "HydroPlant" -> 30; // 30 minutos (asumiendo tiempo intermedio)
            case "NuclearPlant" -> 24 * 60; // 24 horas = 1440 minutos
            case "CoalPlant" -> 8 * 60; // 8 horas = 480 minutos
            case "CombinedCyclePlant" -> 2 * 60; // 2 horas = 120 minutos (más rápido que coal)
            case "FuelGasPlant" -> 4 * 60; // 4 horas = 240 minutos
            case "BiomassPlant" -> 6 * 60; // 6 horas = 360 minutos
            default -> 0;
        };
    }

    private PlantSelection selectOptimalPlants(List<PowerPlants> availablePlants, double demandMW, int minuteOffset,LocalTime currentTime) {
        // Ordenar plantas por prioridad: Solar/Wind -> Nuclear -> Coal/FuelGas
        List<PowerPlants> sortedPlants = new ArrayList<>(availablePlants);
      
        sortedPlants.sort(this::comparePlantPriority);

        PlantSelection selection = new PlantSelection();
        double remainingDemand = demandMW;

        // Seleccionar plantas una por una hasta cubrir la demanda o alcanzar estabilidad mínima
        for (PowerPlants plant : sortedPlants) {
            if (remainingDemand <= 0) break;

            // Calcular cuánta energía puede aportar esta planta
            double plantGeneration = Math.min(getPlantGeneratedMW(plant), remainingDemand);

            // Crear una selección temporal para probar si mantiene la estabilidad
            PlantSelection tempSelection = selection.copy();
            tempSelection.addPlant(plant, plantGeneration);

            // Verificar si la estabilidad promedio se mantiene >= 0.7
            // ¿?¿? Cambiado a 0.5 porque sino no genera el grafico bien tal como esta en el PDF
            if (tempSelection.averageStability >= 0.7 || selection.selectedPlants.isEmpty()) {
                selection = tempSelection;
                remainingDemand -= plantGeneration;
            }
        }

        return selection;
    }

    private int comparePlantPriority(PowerPlants a, PowerPlants b) {
        int priorityA = getPlantPriority(a);
        int priorityB = getPlantPriority(b);

        if (priorityA != priorityB) {
            return Integer.compare(priorityA, priorityB);
        }


        return Double.compare(getPlantStability(b), getPlantStability(a));
    }

    private int getPlantPriority(PowerPlants plant) {
        return switch (plant.getClass().getSimpleName()) {
            case "SolarPlant", "WindPlant" -> 1;
            case "GeothermalPlant", "HydroPlant" -> 2;
            case "NuclearPlant" -> 3;
            case "CombinedCyclePlant" -> 4;
            case "FuelGasPlant" -> 5;
            case "BiomassPlant" -> 6;
            case "CoalPlant" -> 7;
            default -> 8;
        };
    }

    private static class PlantSelection {
        List<PowerPlants> selectedPlants = new ArrayList<>();
        List<Double> plantGenerations = new ArrayList<>();
        double totalGenerated = 0.0;
        double totalStabilityWeight = 0.0;
        double totalWeight = 0.0;
        double averageStability = 0.0;
        Map<String, Double> generatedByType = new HashMap<>();

        void addPlant(PowerPlants plant, double generation) {
            selectedPlants.add(plant);
            plantGenerations.add(generation);
            totalGenerated += generation;

            // Actualizar estabilidad promedio ponderada
            double weight = generation;
            double plantStability = getPlantStability(plant);
            totalStabilityWeight += plantStability * weight;
            totalWeight += weight;

            if (totalWeight > 0) {
                averageStability = totalStabilityWeight / totalWeight;
            }

            // Actualizar generación por tipo
            String plantType = getPlantTypeName(plant);
            generatedByType.put(plantType,
                    generatedByType.getOrDefault(plantType, 0.0) + generation);
        }

        PlantSelection copy() {
            PlantSelection copy = new PlantSelection();
            copy.selectedPlants = new ArrayList<>(this.selectedPlants);
            copy.plantGenerations = new ArrayList<>(this.plantGenerations);
            copy.totalGenerated = this.totalGenerated;
            copy.totalStabilityWeight = this.totalStabilityWeight;
            copy.totalWeight = this.totalWeight;
            copy.averageStability = this.averageStability;
            copy.generatedByType = new HashMap<>(this.generatedByType);
            return copy;
        }

        private String getPlantTypeName(PowerPlants plant) {
            String className = plant.getClass().getSimpleName();
            // Convertir "SolarPlant" a "Solar", etc.
            return className.replace("Plant", "");
        }
    }

    /**
     * Obtiene la estabilidad de una planta mediante downcasting
     * @param plant La planta de la cual obtener la estabilidad
     * @return El valor de estabilidad de la planta
     */
    private static double getPlantStability(PowerPlants plant) {
        switch (plant.getClass().getSimpleName()) {
            case "SolarPlant":
                return ((SolarPlant) plant).getStability();
            case "WindPlant":
                return ((WindPlant) plant).getStability();
            case "GeothermalPlant":
                return ((GeothermalPlant) plant).getStability();
            case "HydroPlant":
                return ((HydroPlant) plant).getStability();
            case "NuclearPlant":
                return ((NuclearPlant) plant).getStability();
            case "CoalPlant":
                return ((CoalPlant) plant).getStability();
            case "CombinedCyclePlant":
                return ((CombinedCyclePlant) plant).getStability();
            case "FuelGasPlant":
                return ((FuelGasPlant) plant).getStability();
            case "BiomassPlant":
                return ((BiomassPlant) plant).getStability();
            default:
                // Valor por defecto si el tipo no es reconocido
                return 0.0;
        }
    }

    /**
     * Obtiene la generación MW de una planta mediante downcasting
     * @param plant La planta de la cual obtener la generación
     * @return El valor de generación MW de la planta
     */
    private double getPlantGeneratedMW(PowerPlants plant) {
        switch (plant.getClass().getSimpleName()) {
            case "SolarPlant":
                return ((SolarPlant) plant).getGeneratedMW();
            case "WindPlant":
                return ((WindPlant) plant).getGeneratedMW();
            case "GeothermalPlant":
                return ((GeothermalPlant) plant).getGeneratedMW();
            case "HydroPlant":
                return ((HydroPlant) plant).getGeneratedMW();
            case "NuclearPlant":
                return ((NuclearPlant) plant).getGeneratedMW();
            case "CoalPlant":
                return ((CoalPlant) plant).getGeneratedMW();
            case "CombinedCyclePlant":
                return ((CombinedCyclePlant) plant).getGeneratedMW();
            case "FuelGasPlant":
                return ((FuelGasPlant) plant).getGeneratedMW();
            case "BiomassPlant":
                return ((BiomassPlant) plant).getGeneratedMW();
            default:
                // Valor por defecto si el tipo no es reconocido
                return 0.0;
        }
    }

    public LinkedList<Result> getSimulationResults(){

        return this.results;
    }
    @Override
    public String toString() {

        if (results == null) simulate();

        JSONArray jsonArray = new JSONArray();
        for (Result result : results) {
            JSONObject obj = new JSONObject();
            obj.put("time", result.getTime().toString());
            obj.put("generatedMW", result.getGeneratedMW());
            obj.put("expectedDemandMW", result.getExpectedDemandMW());
            obj.put("averageStability", result.getAverageStability());

            Map<String, Double> generatedMap = result.getGeneratedByTypeMW();

            Optional.ofNullable(generatedMap)
                    .map(map -> obj.put("generatedByTypeMW", new JSONObject(map)))
                    .orElseGet(() -> obj.put("generatedByTypeMW", JSONObject.NULL));
          /*  if (generatedMap != null) {
                obj.put("generatedByTypeMW", new JSONObject(generatedMap));
            } else {
                obj.put("generatedByTypeMW", JSONObject.NULL);
            }*/

            jsonArray.put(obj);
        }

        return jsonArray.toString(0);
    }
}
