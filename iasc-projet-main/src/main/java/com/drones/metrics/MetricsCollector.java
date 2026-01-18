package com.drones.metrics;

import com.drones.model.Drone;
import com.drones.model.Anomaly;
import com.drones.model.DroneState;
import com.drones.model.Environment;
import java.util.*;

public class MetricsCollector {
    private List<MetricsSnapshot> snapshots;
    private long lastSnapshotTime;
    private static final long SNAPSHOT_INTERVAL = 5000; // 5 seconds
    private Set<Integer> detectedAnomalyIds; // Track which anomalies were detected
    private int totalAnomaliesGenerated; // Total count of all anomalies that appeared
    
    public MetricsCollector() {
        snapshots = new ArrayList<>();
        lastSnapshotTime = 0;
        detectedAnomalyIds = new HashSet<>();
        totalAnomaliesGenerated = 0;
    }
    
    /**
     * Take a snapshot of current metrics
     */
    public void snapshot(List<Drone> drones, Environment env, long time) {
        if (time - lastSnapshotTime >= SNAPSHOT_INTERVAL) {
            MetricsSnapshot snap = new MetricsSnapshot();
            snap.timestamp = time;
            
            // Basic metrics
            snap.activeAnomalies = env.getAnomalies().size();
            snap.activeDrones = (int) drones.stream()
                    .filter(d -> d.getState() == DroneState.ACTIVE)
                    .count();
            snap.rechargingDrones = (int) drones.stream()
                    .filter(d -> d.getState() == DroneState.CHARGING)
                    .count();
            snap.totalMeasurements = (int) drones.stream()
                    .mapToInt(d -> d.getMeasurements().size())
                    .sum();
            
            // Coverage calculation
            double[][] grid = env.getAnomalyIntensity();
            int cellsWithAnomaly = 0;
            for (double[] row : grid) {
                for (double val : row) {
                    if (val > 0.3) cellsWithAnomaly++;
                }
            }
            snap.coveragePercentage = (double) cellsWithAnomaly / (env.getWidth() * env.getHeight()) * 100.0;
            
            // Advanced metrics
            calculateDetectionLatency(snap, env);
            calculateSuccessRate(snap, env);
            calculateEnergyEfficiency(snap, drones);
            calculateMissedAnomalies(snap, env);
            calculateCoordinationScore(snap, drones);
            
            snapshots.add(snap);
            lastSnapshotTime = time;
        }
    }
    
    /**
     * Calculate average detection latency for detected anomalies
     */
    private void calculateDetectionLatency(MetricsSnapshot snap, Environment env) {
        List<Long> latencies = new ArrayList<>();
        
        for (Anomaly a : env.getAnomalies()) {
            if (a.isDetected()) {
                long latency = a.getDetectionLatency();
                if (latency >= 0) {
                    latencies.add(latency);
                }
            }
        }
        
        snap.detectionLatency = latencies.isEmpty() ? 0 : 
                latencies.stream().mapToLong(Long::longValue).average().orElse(0);
    }
    
    /**
     * Calculate success rate (% of anomalies detected before dying)
     */
    private void calculateSuccessRate(MetricsSnapshot snap, Environment env) {
        if (totalAnomaliesGenerated == 0) {
            snap.successRate = 0;
            return;
        }
        
        int detectedCount = 0;
        for (Anomaly a : env.getAnomalies()) {
            if (a.isDetected()) {
                detectedCount++;
                detectedAnomalyIds.add(System.identityHashCode(a));
            }
        }
        
        snap.successRate = (detectedCount * 100.0) / totalAnomaliesGenerated;
    }
    
    /**
     * Calculate energy efficiency (coverage per unit of energy consumed)
     */
    private void calculateEnergyEfficiency(MetricsSnapshot snap, List<Drone> drones) {
        double totalEnergyConsumed = drones.stream()
                .mapToDouble(Drone::getTotalEnergyConsumed)
                .sum();
        
        if (totalEnergyConsumed == 0) {
            snap.energyEfficiency = 0;
            return;
        }
        
        snap.energyEfficiency = snap.coveragePercentage / (totalEnergyConsumed / 100.0);
    }
    
    /**
     * Calculate missed anomalies (count of undetected anomalies)
     */
    private void calculateMissedAnomalies(MetricsSnapshot snap, Environment env) {
        snap.missedAnomalies = (int) env.getAnomalies().stream()
                .filter(a -> !a.isDetected())
                .count();
    }
    
    /**
     * Calculate coordination score based on drone spread and efficiency
     * Higher score = better coordination
     */
    private void calculateCoordinationScore(MetricsSnapshot snap, List<Drone> drones) {
        if (drones.isEmpty()) {
            snap.coordinationScore = 0;
            return;
        }
        
        // Coordination factors:
        // 1. Active drones ratio (more active = better)
        double activeRatio = snap.activeDrones / (double) drones.size();
        
        // 2. Recharged drones ratio (cycling properly = better)
        double rechargeRatio = snap.rechargingDrones / (double) drones.size();
        
        // 3. Measurement efficiency (more measurements = better coordination)
        double measurementEff = Math.min(1.0, snap.totalMeasurements / 100.0);
        
        // Combined score: 0-100
        snap.coordinationScore = (activeRatio * 40 + rechargeRatio * 30 + measurementEff * 30);
    }
    
    /**
     * Update total anomalies count (call when new anomaly is spawned)
     */
    public void recordAnomalySpawned() {
        totalAnomaliesGenerated++;
    }
    
    public List<MetricsSnapshot> getSnapshots() {
        return snapshots;
    }
    
    public void reset() {
        snapshots.clear();
        lastSnapshotTime = 0;
        detectedAnomalyIds.clear();
        totalAnomaliesGenerated = 0;
    }
}
