package edu.uoc.uoctron.model;

import java.time.LocalDateTime;
import java.util.*;

public class SimulationResults {

    LinkedList<Result> results;
    public static class Result {
        private LocalDateTime time;
        private double generatedMW;
        private double expectedDemand;
        private double averageStability;
        private Map<String, Double> generatedByTypeMW;

        public Result(LocalDateTime time, double generatedMW, double expectedDemand,
                      double averageStability, Map<String, Double> generatedByTypeMW) {
            this.time = time;
            this.generatedMW = generatedMW;
            this.expectedDemand = expectedDemand;
            this.averageStability = averageStability;
            this.generatedByTypeMW = new HashMap<>(generatedByTypeMW);
        }

        // Getters
        public LocalDateTime getTime() { return time; }
        public double getGeneratedMW() { return generatedMW; }
        public double getExpectedDemand() { return expectedDemand; }
        public double getAverageStability() { return averageStability; }
        public Map<String, Double> getGeneratedByTypeMW() { return generatedByTypeMW; }

    }

    private LocalDateTime blackoutStart;
    private LinkedList<PowerPlants> powerPlants;
    private LinkedList<DemandMinute> demandMinutes;

    public SimulationResults(LocalDateTime blackoutStart,
                             LinkedList<PowerPlants> powerPlants,
                             LinkedList<DemandMinute> demandMinutes) {
        this.blackoutStart = blackoutStart;
        this.powerPlants = powerPlants;
        this.demandMinutes = demandMinutes;
    }

    public LinkedList<Result> simulate() {
        LinkedList<Result> results1 = new LinkedList<>();

        // Simular para las siguientes 36 horas (2160 minutos)
        for (int minuteOffset = 0; minuteOffset < 2160; minuteOffset++) {
            LocalDateTime currentTime = blackoutStart.plusMinutes(minuteOffset);

            // Obtener la demanda para este minuto
            double expectedDemand = getDemandForMinute(minuteOffset);

            // Obtener plantas disponibles en este momento
            List<PowerPlants> availablePlants = getAvailablePlants(minuteOffset);

            // Seleccionar plantas óptimas para cubrir la demanda
            PlantSelection selection = selectOptimalPlants(availablePlants, expectedDemand);

            // Crear el resultado para este minuto
            Result result = new Result(
                    currentTime,
                    selection.totalGenerated,
                    expectedDemand,
                    selection.averageStability,
                    selection.generatedByType
            );

            results1.add(result);
        }

        return results1;
    }

    private double getDemandForMinute(int minuteOffset) {
        // Calcular el índice en el LinkedList de demandMinutes
        // Asumiendo que demandMinutes contiene 1440 minutos (24 horas) que se repiten
        int demandIndex = minuteOffset % 1440;

        if (demandIndex < demandMinutes.size()) {
            return demandMinutes.get(demandIndex).getDemand();
        }
        return 0.0; // Valor por defecto si no hay datos
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
        switch (plant.getClass().getSimpleName()) {
            case "SolarPlant":
            case "WindPlant":
                return 6; // 6 minutos
            case "GeothermalPlant":
            case "HydroPlant":
                return 30; // 30 minutos (asumiendo tiempo intermedio)
            case "NuclearPlant":
                return 24 * 60; // 24 horas = 1440 minutos
            case "CoalPlant":
                return 8 * 60; // 8 horas = 480 minutos
            case "CombinedCyclePlant":
                return 2 * 60; // 2 horas = 120 minutos (más rápido que coal)
            case "FuelGasPlant":
                return 4 * 60; // 4 horas = 240 minutos
            case "BiomassPlant":
                return 6 * 60; // 6 horas = 360 minutos
            default:
                return 0;
        }
    }

    private PlantSelection selectOptimalPlants(List<PowerPlants> availablePlants, double demandMW) {
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

        // Si tienen la misma prioridad, ordenar por estabilidad (mayor estabilidad primero)
        return Double.compare(getPlantStability(b), getPlantStability(a));
    }

    private int getPlantPriority(PowerPlants plant) {
        switch (plant.getClass().getSimpleName()) {
            case "SolarPlant":
            case "WindPlant":
                return 1; // Máxima prioridad - Renovables rápidas
            case "GeothermalPlant":
            case "HydroPlant":
                return 2; // Segunda prioridad - Renovables estables
            case "NuclearPlant":
                return 3; // Tercera prioridad - Energía base limpia
            case "CombinedCyclePlant":
                return 4; // Cuarta prioridad - Gas eficiente
            case "FuelGasPlant":
                return 5; // Quinta prioridad - Gas convencional
            case "BiomassPlant":
                return 6; // Sexta prioridad - Biomasa
            case "CoalPlant":
                return 7; // Menor prioridad - Carbón
            default:
                return 8;
        }
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

    @Override
    public String toString() {
        StringBuffer resultsLoaded = new StringBuffer();
        String resultsLoadedToString;
        LinkedList<Result> resultshere = this.simulate();
        for (int i = 0; i < resultshere.size(); i++){
            resultsLoaded.append( "{\n" +
                    "\"current_time\": " + "tests" + ",\n" +
                    "\"generatedMW\": " + resultshere.get(i).getGeneratedMW() + ",\n" +
                    "\"expectedDemand\": " + resultshere.get(i).getExpectedDemand() + ",\n" +
                    "\"averageStability\": " + resultshere.get(i).getAverageStability() + ",\n"+
                    "\"generatedByType\" : { \"type\": " + resultshere.get(i).getClass().getName() + ",\n\"energy_generated\": " + resultshere.get(i).getGeneratedByTypeMW().get(i) + "\n}\n}");

        }
        resultsLoadedToString = resultsLoaded.toString();
        return resultsLoadedToString;
    }
}
