package com.drones.metrics;

/**
 * Snapshot of simulation metrics at a specific point in time
 */
public class MetricsSnapshot {
    // Basic metrics
    public long timestamp;
    public double coveragePercentage;
    public int activeAnomalies;
    public int activeDrones;
    public int rechargingDrones;
    public int totalMeasurements;
    
    // Advanced metrics
    public double detectionLatency; // Average time to detect anomalies in ms
    public double successRate; // % of anomalies detected before they die
    public double energyEfficiency; // coverage% / avgEnergyConsumed
    public int missedAnomalies; // Anomalies that disappeared undetected
    public double coordinationScore; // 0-100, measure of drone coordination
    
    public MetricsSnapshot() {
        this.timestamp = 0;
        this.coveragePercentage = 0;
        this.activeAnomalies = 0;
        this.activeDrones = 0;
        this.rechargingDrones = 0;
        this.totalMeasurements = 0;
        this.detectionLatency = 0;
        this.successRate = 0;
        this.energyEfficiency = 0;
        this.missedAnomalies = 0;
        this.coordinationScore = 0;
    }
    
    @Override
    public String toString() {
        return String.format(
            "MetricsSnapshot{time=%d, coverage=%.1f%%, anomalies=%d, activeDrones=%d, " +
            "latency=%.0fms, successRate=%.1f%%, energyEff=%.2f, missed=%d, coord=%.1f}",
            timestamp, coveragePercentage, activeAnomalies, activeDrones,
            detectionLatency, successRate, energyEfficiency, missedAnomalies, coordinationScore
        );
    }
}
