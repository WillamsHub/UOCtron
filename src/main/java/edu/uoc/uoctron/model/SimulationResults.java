package edu.uoc.uoctron.model;

import edu.uoc.uoctron.utils.Utils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import java.util.*;

public class SimulationResults {
    public LocalDateTime blackoutStart;
    public LinkedList<PowerPlants> powerPlants;
    public LinkedList<DemandMinute> demandMinutes;
    public List<Result> results;
    private static final double MIN_STABILITY = 0.7;
    public LocalDateTime currentTime;

    public SimulationResults(LocalDateTime blackoutStart, LinkedList<PowerPlants> powerPlants, LinkedList<DemandMinute> demandMinutes) {
        this.blackoutStart = blackoutStart;
        this.powerPlants = powerPlants;
        this.demandMinutes = demandMinutes;
        this.results = new ArrayList<>();
    }

    public List<Result> runSimulation() {
        return simulate();
    }

    public List<Result> simulate() {
        results.clear();
        LocalDateTime currentTime = blackoutStart;
        int demandIndex = 0;

        // Simular 36 horas (2160 minutos)
        for (int minute = 0; minute < 2160; minute++) {
            // Obtener la demanda actual, ciclando a través de las 24 horas (1440 minutos)
            DemandMinute demand = demandMinutes.get(demandIndex);

            // Filtrar plantas disponibles para este momento
            List<PowerPlants> availablePlants = getAvailablePlants(currentTime);

            // Seleccionar plantas para cubrir la demanda excluyendo tipos específicos
            List<PowerPlants> selectedPlants = selectPlantsGreedy(availablePlants, demand.getDemand(), currentTime);

            // Calcular resultados
            double generatedMW = calculateTotalGeneration(selectedPlants);
            double averageStability = calculateWeightedStability(selectedPlants);
            Map<String, Double> generatedByType = calculateGenerationByType(selectedPlants);

            // Crear resultado para este minuto
            Result result = new Result(currentTime, generatedMW, demand.getDemand(), averageStability, generatedByType);
            results.add(result);

            // Avanzar al siguiente minuto
            currentTime = currentTime.plusMinutes(1);

            // Avanzar el índice de demanda y reiniciar al principio después de 1440 minutos (24 horas)
            demandIndex = (demandIndex + 1) % 1440;
        }

        return results;
    }

    private List<PowerPlants> getAvailablePlants(LocalDateTime currentTime) {
        List<PowerPlants> available = new ArrayList<>();

        for (PowerPlants plant : powerPlants) {
            if (isPlantOperational(plant, currentTime) && isPlantAllowed(plant)) {
                available.add(plant);
            }
        }

        return available;
    }

    private boolean isPlantAllowed(PowerPlants plant) {
        // Excluir específicamente Biomass, Nuclear y FuelGas
        return !(plant instanceof BiomassPlant ||
                plant instanceof NuclearPlant ||
                plant instanceof FuelGasPlant);
    }

    private boolean isPlantOperational(PowerPlants plant, LocalDateTime currentTime) {
        // Verificar si la planta ha superado el tiempo de reinicio tras blackout
        LocalTime restartTime = plant.getRestartTime();
        LocalDateTime restartDateTime = blackoutStart.toLocalDate().atTime(restartTime);

        // Si el tiempo de reinicio es menor que la hora del blackout, añadir un día
        if (restartTime.isBefore(blackoutStart.toLocalTime())) {
            restartDateTime = restartDateTime.plusDays(1);
        }

        if (currentTime.isBefore(restartDateTime)) {
            return false;
        }

        // Verificar horario de operación
        int hour = currentTime.getHour();
        int minute = currentTime.getMinute();
        int totalMinutes = hour * 60 + minute;

        // Plantas solares: 07:00-18:59
        if (plant instanceof SolarPlant) {
            return totalMinutes >= 420 && totalMinutes <= 1139; // 7*60 a 18*60+59
        }

        // Todas las demás plantas: 00:00-23:59
        return true;
    }

    private List<PowerPlants> selectPlantsGreedy(List<PowerPlants> availablePlants, double demandMW, LocalDateTime currentTime) {
        List<PowerPlants> selectedPlants = new ArrayList<>();
        double currentGeneration = 0.0;

        // Crear copia de plantas disponibles para no modificar la original
        List<PowerPlants> remainingPlants = new ArrayList<>(availablePlants);

        // Algoritmo greedy: seleccionar plantas hasta cubrir la demanda
        while (currentGeneration < demandMW && !remainingPlants.isEmpty()) {
            PowerPlants bestPlant = null;
            double bestScore = -1;

            // Encontrar la mejor planta basada en ratio eficiencia/capacidad y tipo
            for (PowerPlants plant : remainingPlants) {
                // Verificar nuevamente que la planta está permitida
                if (!isPlantAllowed(plant)) {
                    continue;
                }

                double score = calculatePlantScore(plant, demandMW - currentGeneration);
                if (score > bestScore) {
                    bestScore = score;
                    bestPlant = plant;
                }
            }

            if (bestPlant != null) {
                selectedPlants.add(bestPlant);
                currentGeneration += bestPlant.getMaxCapacityMW() * bestPlant.getEfficiency();
                remainingPlants.remove(bestPlant);
            } else {
                break;
            }
        }

        // Verificar y ajustar para estabilidad mínima
        if (calculateWeightedStability(selectedPlants) < MIN_STABILITY) {
            selectedPlants = ensureMinimumStability(selectedPlants, remainingPlants, demandMW);
        }

        return selectedPlants;
    }

    private double calculatePlantScore(PowerPlants plant, double remainingDemand) {
        double generation = plant.getMaxCapacityMW() * plant.getEfficiency();
        double efficiency = plant.getEfficiency();
        double stability = plant.getStability();

        // Score base: generación * eficiencia * estabilidad
        double baseScore = generation * efficiency * stability;

        // Bonus por tipo de planta (preferencias estratégicas)
        double typeBonus = 1.0;
        if (plant instanceof HydroPlant) {
            typeBonus = 2.0; // Renovable y muy estable
        } else if (plant instanceof CombinedCyclePlant) {
            typeBonus = 1.8; // Muy eficiente
        } else if (plant instanceof CoalPlant) {
            typeBonus = 1.5; // Base load confiable
        } else if (plant instanceof WindPlant) {
            typeBonus = 1.6; // Renovable
        } else if (plant instanceof SolarPlant) {
            typeBonus = 1.4; // Renovable pero limitada
        } else if (plant instanceof GeothermalPlant) {
            typeBonus = 1.3; // Estable
        } else {
            typeBonus = 1.0; // Otros tipos permitidos
        }

        // Penalizar si la planta genera mucho más de lo necesario
        if (generation > remainingDemand * 2) {
            typeBonus *= 0.7;
        }

        return baseScore * typeBonus;
    }

    private List<PowerPlants> ensureMinimumStability(List<PowerPlants> selectedPlants, List<PowerPlants> availablePlants, double demandMW) {
        List<PowerPlants> adjusted = new ArrayList<>(selectedPlants);
        List<PowerPlants> remaining = new ArrayList<>();

        // Filtrar plantas restantes para incluir solo las permitidas
        for (PowerPlants plant : availablePlants) {
            if (isPlantAllowed(plant)) {
                remaining.add(plant);
            }
        }

        // Ordenar plantas restantes por estabilidad (mayor primero)
        remaining.sort((a, b) -> Double.compare(b.getStability(), a.getStability()));

        // Añadir plantas con alta estabilidad hasta alcanzar el mínimo
        for (PowerPlants plant : remaining) {
            adjusted.add(plant);
            if (calculateWeightedStability(adjusted) >= MIN_STABILITY) {
                break;
            }
        }

        return adjusted;
    }

    private double calculateTotalGeneration(List<PowerPlants> plants) {
        return plants.stream().mapToDouble(plant -> plant.getMaxCapacityMW() * plant.getEfficiency()).sum();
    }

    private double calculateWeightedStability(List<PowerPlants> plants) {
        if (plants.isEmpty()) return 0;

        double totalGeneration = calculateTotalGeneration(plants);
        if (totalGeneration == 0) return 0;

        double weightedStability = 0;
        for (PowerPlants plant : plants) {
            double plantGeneration = plant.getMaxCapacityMW() * plant.getEfficiency();
            double weight = plantGeneration / totalGeneration;
            weightedStability += plant.getStability() * weight;
        }

        return weightedStability;
    }

    private Map<String, Double> calculateGenerationByType(List<PowerPlants> plants) {
        Map<String, Double> generationByType = new HashMap<>();

        for (PowerPlants plant : plants) {
            String type = plant.getClass().getSimpleName().replace("Plant", "");
            if (type.equals("Hydro")){
                type += "electric";
            }
            type = Utils.fromCamelOrPascalToSentence(type);

            // Calcular la generación real de la planta (capacidad * eficiencia)
            double plantGeneration = plant.getMaxCapacityMW() * plant.getEfficiency();
            generationByType.merge(type, plantGeneration, Double::sum);
        }

        return generationByType;
    }

    @Override
    public String toString() {
        StringBuilder jsonBuilder = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        jsonBuilder.append("[\n");

        for (int i = 0; i < results.size(); i++) {
            Result result = results.get(i);
            jsonBuilder.append("  {\n");
            jsonBuilder.append("    \"minute\": ").append(i + 1).append(",\n");
            jsonBuilder.append("    \"timestamp\": \"").append(result.getTime().format(formatter)).append("\",\n");
            jsonBuilder.append("    \"generatedMW\": ").append(String.format("%.2f", result.getGeneratedMW())).append(",\n");
            jsonBuilder.append("    \"expectedDemandMW\": ").append(String.format("%.2f", result.getExpectedDemandMW())).append(",\n");
            jsonBuilder.append("    \"averageStability\": ").append(String.format("%.3f", result.getAverageStability())).append(",\n");
            jsonBuilder.append("    \"generatedByType\": {\n");

            Map<String, Double> genByType = result.getGeneratedByTypeMW();
            int typeCount = 0;
            for (Map.Entry<String, Double> entry : genByType.entrySet()) {

                jsonBuilder.append("      \"").append(entry.getKey()).append("\": ")
                        .append(String.format("%.2f", entry.getValue()));
                if (++typeCount < genByType.size()) {
                    jsonBuilder.append(",");
                }
                jsonBuilder.append("\n");
            }

            jsonBuilder.append("    }\n");
            jsonBuilder.append("  }");
            if (i < results.size() - 1) {
                jsonBuilder.append(",");
            }
            jsonBuilder.append("\n");
        }

        jsonBuilder.append("]");
        return jsonBuilder.toString();
    }

    // Getters
    public List<Result> getResults() {
        return results;
    }

    public LocalDateTime getBlackoutStart() {
        return blackoutStart;
    }
}