package edu.uoc.uoctron.model;

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
        this.currentTime = blackoutStart;

        for (DemandMinute demand : demandMinutes) {
            // Filtrar plantas disponibles para este momento
            List<PowerPlants> availablePlants = getAvailablePlants(currentTime);

            // Seleccionar plantas para cubrir la demanda
            List<PowerPlants> selectedPlants = selectPlantsForDemand(availablePlants, demand.getDemand(), currentTime);

            // Calcular resultados
            double generatedMW = calculateTotalGeneration(selectedPlants);
            double averageStability = calculateWeightedStability(selectedPlants);
            Map<String, Double> generatedByType = calculateGenerationByType(selectedPlants);

            // Crear resultado para este minuto
            Result result = new Result(currentTime, generatedMW, demand.getDemand(), averageStability, generatedByType);
            results.add(result);

            // Avanzar al siguiente minuto
            currentTime = currentTime.plusMinutes(1);
        }

        return results;
    }

    private List<PowerPlants> getAvailablePlants(LocalDateTime currentTime) {
        List<PowerPlants> available = new ArrayList<>();

        for (PowerPlants plant : powerPlants) {
            if (isPlantOperational(plant, currentTime)) {
                available.add(plant);
            }
        }

        return available;
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

    private List<PowerPlants> selectPlantsForDemand(List<PowerPlants> availablePlants, double demandMW, LocalDateTime currentTime) {
        // Ordenar plantas por prioridad
        List<PowerPlants> renewables = new ArrayList<>();
        List<PowerPlants> nuclear = new ArrayList<>();
        List<PowerPlants> thermal = new ArrayList<>();

        for (PowerPlants plant : availablePlants) {
            if (plant instanceof SolarPlant || plant instanceof WindPlant ||
                    plant instanceof GeothermalPlant || plant instanceof HydroPlant) {
                renewables.add(plant);
            } else if (plant instanceof NuclearPlant) {
                nuclear.add(plant);
            } else {
                thermal.add(plant);
            }
        }

        // Ordenar por capacidad máxima (descendente) dentro de cada categoría
        renewables.sort((a, b) -> Double.compare(b.getMaxCapacityMW(), a.getMaxCapacityMW()));
        nuclear.sort((a, b) -> Double.compare(b.getMaxCapacityMW(), a.getMaxCapacityMW()));
        thermal.sort((a, b) -> Double.compare(b.getMaxCapacityMW(), a.getMaxCapacityMW()));

        List<PowerPlants> selectedPlants = new ArrayList<>();
        double currentGeneration = 0;

        // 1. Añadir renovables primero
        for (PowerPlants plant : renewables) {
            if (currentGeneration < demandMW) {
                selectedPlants.add(plant);
                currentGeneration += plant.getMaxCapacityMW();
            }
        }

        // 2. Añadir nucleares si es necesario
        if (currentGeneration < demandMW) {
            for (PowerPlants plant : nuclear) {
                if (currentGeneration < demandMW) {
                    // Verificar si añadir esta planta sobrepasa la demanda
                    double newGeneration = currentGeneration + plant.getMaxCapacityMW();
                    if (newGeneration > demandMW) {
                        // Quitar renovables hasta que se ajuste
                        selectedPlants = adjustSelection(selectedPlants, plant, demandMW);
                        break;
                    } else {
                        selectedPlants.add(plant);
                        currentGeneration = newGeneration;
                    }
                }
            }
        }

        // 3. Añadir térmicas si es necesario
        currentGeneration = calculateTotalGeneration(selectedPlants);
        if (currentGeneration < demandMW) {
            for (PowerPlants plant : thermal) {
                if (currentGeneration < demandMW) {
                    double newGeneration = currentGeneration + plant.getMaxCapacityMW();
                    if (newGeneration > demandMW) {
                        selectedPlants = adjustSelection(selectedPlants, plant, demandMW);
                        break;
                    } else {
                        selectedPlants.add(plant);
                        currentGeneration = newGeneration;
                    }
                }
            }
        }

        // Verificar que la estabilidad sea >= 70%
        if (calculateWeightedStability(selectedPlants) < MIN_STABILITY) {
            selectedPlants = adjustForStability(selectedPlants, demandMW);
        }

        return selectedPlants;
    }

    private List<PowerPlants> adjustSelection(List<PowerPlants> currentSelection, PowerPlants newPlant, double demandMW) {
        List<PowerPlants> adjusted = new ArrayList<>(currentSelection);
        adjusted.add(newPlant);

        // Quitar renovables hasta que la generación no sobrepase la demanda
        double totalGeneration = calculateTotalGeneration(adjusted);

        Iterator<PowerPlants> iterator = adjusted.iterator();
        while (iterator.hasNext() && totalGeneration > demandMW) {
            PowerPlants plant = iterator.next();
            if (plant instanceof SolarPlant || plant instanceof WindPlant ||
                    plant instanceof GeothermalPlant || plant instanceof HydroPlant) {

                double generationWithoutThis = totalGeneration - plant.getMaxCapacityMW();
                if (generationWithoutThis >= demandMW * 0.95) { // Mantener al menos 95% de la demanda
                    iterator.remove();
                    totalGeneration = generationWithoutThis;
                }
            }
        }

        return adjusted;
    }

    private List<PowerPlants> adjustForStability(List<PowerPlants> selectedPlants, double demandMW) {
        // Implementar lógica para ajustar selección manteniendo estabilidad >= 70%
        // Esta es una implementación simplificada
        List<PowerPlants> adjusted = new ArrayList<>(selectedPlants);

        // Ordenar por estabilidad descendente
        adjusted.sort((a, b) -> Double.compare(b.getStability(), a.getStability()));

        // Reconstruir selección priorizando plantas con mayor estabilidad
        List<PowerPlants> newSelection = new ArrayList<>();
        double currentGeneration = 0;

        for (PowerPlants plant : adjusted) {
            newSelection.add(plant);
            currentGeneration += plant.getMaxCapacityMW();

            if (calculateWeightedStability(newSelection) >= MIN_STABILITY &&
                    currentGeneration >= demandMW * 0.95) {
                break;
            }
        }

        return newSelection;
    }

    private double calculateTotalGeneration(List<PowerPlants> plants) {
        return plants.stream().mapToDouble(PowerPlants::getMaxCapacityMW).sum();
    }

    private double calculateWeightedStability(List<PowerPlants> plants) {
        if (plants.isEmpty()) return 0;

        double totalGeneration = calculateTotalGeneration(plants);
        if (totalGeneration == 0) return 0;

        double weightedStability = 0;
        int index = 0;
        for (PowerPlants plant : plants) {
            double weight = plant.getMaxCapacityMW() / totalGeneration;
            weightedStability += powerPlants.get(index).getStability() * weight;
            index ++;
        }

        return weightedStability;
    }

    private Map<String, Double> calculateGenerationByType(List<PowerPlants> plants) {
        Map<String, Double> generationByType = new HashMap<>();

        for (PowerPlants plant : plants) {
            String type = plant.getClass().getSimpleName().replace("Plant", "");
            generationByType.merge(type, plant.getMaxCapacityMW(), Double::sum);
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