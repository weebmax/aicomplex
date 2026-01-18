package com.drones.control;

import com.drones.config.SimulationParams;
import com.drones.model.*;
import com.drones.metrics.MetricsCollector;
import com.drones.metrics.MetricsSnapshot;
import java.util.*;

public class SimulationEngine {
    private Environment environment;
    private List<Drone> drones;
    private long simulationTime;
    private boolean running;
    private MetricsCollector metricsCollector;
    private Coordinator coordinator;
    private int tickCount;
    private Set<Integer> spawnedAnomalyIds; // Track spawned anomalies
    
    public SimulationEngine() {
        this.environment = new Environment(SimulationParams.GRID_WIDTH, SimulationParams.GRID_HEIGHT);
        this.drones = new ArrayList<>();
        this.simulationTime = 0;
        this.running = false;
        this.metricsCollector = new MetricsCollector();
        this.coordinator = new Coordinator();
        this.tickCount = 0;
        this.spawnedAnomalyIds = new HashSet<>();
        
        // Initialize drones at base (0, 0)
        for (int i = 0; i < SimulationParams.NUM_DRONES; i++) {
            drones.add(new Drone(i, 0, 0));
        }
        
        // Initialize waypoints (simple raster scan)
        initializeCoverageWaypoints();
    }
    
    private void initializeCoverageWaypoints() {
        int dronesPerRow = (int) Math.ceil(Math.sqrt(SimulationParams.NUM_DRONES));
        int cellsPerDrone = SimulationParams.GRID_WIDTH / dronesPerRow;
        
        for (int i = 0; i < drones.size(); i++) {
            List<double[]> waypoints = new ArrayList<>();
            
            // Assign a region to each drone
            int row = i / dronesPerRow;
            int col = i % dronesPerRow;
            
            int startX = col * cellsPerDrone;
            int startY = row * cellsPerDrone;
            int endX = Math.min((col + 1) * cellsPerDrone, SimulationParams.GRID_WIDTH);
            int endY = Math.min((row + 1) * cellsPerDrone, SimulationParams.GRID_HEIGHT);
            
            // Raster scan pattern
            for (int y = startY; y < endY; y++) {
                if ((y - startY) % 2 == 0) {
                    for (int x = startX; x < endX; x++) {
                        waypoints.add(new double[]{x, y});
                    }
                } else {
                    for (int x = endX - 1; x >= startX; x--) {
                        waypoints.add(new double[]{x, y});
                    }
                }
            }
            
            // Go back to base
            waypoints.add(new double[]{0, 0});
            
            drones.get(i).setWaypoints(waypoints);
        }
    }
    
    public void tick() {
        if (!running) return;
        
        // Track previous anomaly count
        int previousAnomalyCount = environment.getAnomalies().size();
        
        // Update environment
        environment.update(SimulationParams.TICK_DURATION_MS);
        
        // Track new anomalies spawned
        int currentAnomalyCount = environment.getAnomalies().size();
        if (currentAnomalyCount > previousAnomalyCount) {
            for (int i = 0; i < currentAnomalyCount - previousAnomalyCount; i++) {
                metricsCollector.recordAnomalySpawned();
            }
        }
        
        // Update drones
        for (Drone drone : drones) {
            drone.update(SimulationParams.TICK_DURATION_MS);
            
            // If drone is active and detects anomaly at position
            if (drone.getState() == DroneState.ACTIVE) {
                double intensity = environment.getAnomalyAt(drone.getX(), drone.getY());
                if (intensity > SimulationParams.ANOMALY_DETECTION_THRESHOLD) {
                    // Add noise to measurement
                    double measured = intensity + (Math.random() - 0.5) * 0.1;
                    drone.addMeasurement(measured, simulationTime, drone.getX(), drone.getY());
                    
                    // Mark anomaly as detected
                    for (Anomaly anomaly : environment.getAnomalies()) {
                        double dist = Math.sqrt(
                            Math.pow(anomaly.getX() - drone.getX(), 2) +
                            Math.pow(anomaly.getY() - drone.getY(), 2)
                        );
                        if (dist < 1.0 && !anomaly.isDetected()) {
                            drone.detectAnomalyAt(anomaly, simulationTime);
                        }
                    }
                }
            }
        }
        
        // Adaptive re-tasking every 30 ticks (6 seconds)
        if (tickCount++ % 30 == 0) {
            coordinator.adaptiveRetasking(drones, environment);
        }
        
        // Update metrics
        metricsCollector.snapshot(drones, environment, simulationTime);
        
        simulationTime += SimulationParams.TICK_DURATION_MS;
    }
    
    public void start() {
        this.running = true;
    }
    
    public void stop() {
        this.running = false;
    }
    
    public void reset() {
        environment.reset();
        for (Drone d : drones) {
            d.clearMeasurements();
        }
        simulationTime = 0;
        tickCount = 0;
        running = false;
        metricsCollector.reset();
        coordinator.reset();
        spawnedAnomalyIds.clear();
        initializeCoverageWaypoints();
    }
    
    public Environment getEnvironment() { return environment; }
    public List<Drone> getDrones() { return drones; }
    public long getSimulationTime() { return simulationTime; }
    public boolean isRunning() { return running; }
    public MetricsCollector getMetricsCollector() { return metricsCollector; }
    public List<MetricsSnapshot> getMetricsSnapshots() { return metricsCollector.getSnapshots(); }
    
    // Deprecated: Use MetricsCollector instead
    @Deprecated
    public static class SimulationMetrics {
        public double coveragePercentage;
        public int anomaliesDetected;
        public double averageDetectionTime;
        public int activeDrones;
        public int rechargingDrones;
        
        public void update(List<Drone> drones, Environment env, long time) {
            // Count active/charging
            activeDrones = (int) drones.stream().filter(d -> d.getState() == DroneState.ACTIVE).count();
            rechargingDrones = (int) drones.stream().filter(d -> d.getState() == DroneState.CHARGING).count();
            
            // Count cells with anomalies detected
            double[][] grid = env.getAnomalyIntensity();
            int cellsWithAnomaly = 0;
            for (double[] row : grid) {
                for (double val : row) {
                    if (val > SimulationParams.ANOMALY_DETECTION_THRESHOLD) {
                        cellsWithAnomaly++;
                    }
                }
            }
            
            int totalCells = env.getWidth() * env.getHeight();
            coveragePercentage = (double) cellsWithAnomaly / totalCells * 100.0;
            
            anomaliesDetected = env.getAnomalies().size();
        }
        
        public void reset() {
            coveragePercentage = 0;
            anomaliesDetected = 0;
            averageDetectionTime = 0;
            activeDrones = 0;
            rechargingDrones = 0;
        }
    }
}
