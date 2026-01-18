package com.drones.model;

import com.drones.config.SimulationParams;
import java.util.*;

public class Drone {
    private int id;
    private double x, y;
    private double targetX, targetY;
    private DroneState state;
    private long autonomyRemaining; // ms
    private long measurementTimer; // ms, counts down during measurement
    private long rechargingTimer; // ms, counts down during recharge
    private List<Measurement> measurements;
    private Deque<double[]> waypoints; // queue of (x,y) targets
    private double totalEnergyConsumed; // Track total energy consumption
    
    public Drone(int id, double startX, double startY) {
        this.id = id;
        this.x = startX;
        this.y = startY;
        this.targetX = startX;
        this.targetY = startY;
        this.state = DroneState.ACTIVE;
        this.autonomyRemaining = SimulationParams.DRONE_AUTONOMY_MS;
        this.measurementTimer = 0;
        this.rechargingTimer = 0;
        this.measurements = new ArrayList<>();
        this.waypoints = new ArrayDeque<>();
        this.totalEnergyConsumed = 0;
    }
    
    public int getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public DroneState getState() { return state; }
    public long getAutonomyRemaining() { return autonomyRemaining; }
    public List<Measurement> getMeasurements() { return measurements; }
    public double getTotalEnergyConsumed() { return totalEnergyConsumed; }
    
    // Add a measurement (from sensor reading)
    public void addMeasurement(double intensity, long timestamp, double x, double y) {
        measurements.add(new Measurement(intensity, timestamp, x, y));
    }
    
    // Clear local measurements (upload to base)
    public void clearMeasurements() {
        measurements.clear();
    }
    
    // Set waypoints for planned path
    public void setWaypoints(List<double[]> points) {
        waypoints.clear();
        waypoints.addAll(points);
    }
    
    // Get next waypoint
    private boolean updateTargetWaypoint() {
        if (waypoints.isEmpty()) {
            return false;
        }
        double[] next = waypoints.peek();
        targetX = next[0];
        targetY = next[1];
        
        // Check if reached target
        double dist = Math.sqrt(Math.pow(x - targetX, 2) + Math.pow(y - targetY, 2));
        if (dist < 0.5) {
            waypoints.poll();
            return !waypoints.isEmpty();
        }
        return true;
    }
    
    // Move towards target
    private void moveToward(double tx, double ty, double tickDurationS) {
        double dist = Math.sqrt(Math.pow(tx - x, 2) + Math.pow(ty - y, 2));
        if (dist < 0.1) return; // already there
        
        double speed = SimulationParams.DRONE_SPEED;
        double moveDistance = speed * tickDurationS;
        double ratio = Math.min(1.0, moveDistance / dist);
        
        x += (tx - x) * ratio;
        y += (ty - y) * ratio;
    }
    
    // Update drone state each tick
    public void update(long tickDurationMs) {
        double tickDurationS = tickDurationMs / 1000.0;
        
        // Track energy consumption
        double energyPerTick = 0;
        
        switch (state) {
            case ACTIVE:
                energyPerTick = 1.0; // Energy per tick during active exploration
                // Update waypoint if needed
                updateTargetWaypoint();
                // Move towards target
                moveToward(targetX, targetY, tickDurationS);
                // Consume autonomy
                autonomyRemaining -= tickDurationMs;
                if (autonomyRemaining <= 0) {
                    setState(DroneState.RETURNING);
                    targetX = 0;
                    targetY = 0;
                }
                break;
                
            case MEASURING:
                energyPerTick = 1.5; // Higher energy during measurement
                // Count down measurement
                measurementTimer -= tickDurationMs;
                autonomyRemaining -= tickDurationMs;
                if (measurementTimer <= 0) {
                    setState(DroneState.ACTIVE);
                }
                if (autonomyRemaining <= 0) {
                    setState(DroneState.RETURNING);
                }
                break;
                
            case RETURNING:
                energyPerTick = 0.8; // Less energy during return (no sensor)
                // Move back to base (0, 0)
                double dist = Math.sqrt(x * x + y * y);
                if (dist < 0.5) {
                    // Reached base
                    setState(DroneState.CHARGING);
                    rechargingTimer = SimulationParams.DRONE_RECHARGE_MS;
                    measurements.clear(); // upload to base
                } else {
                    moveToward(0, 0, tickDurationS);
                    autonomyRemaining -= tickDurationMs;
                }
                break;
                
            case CHARGING:
                energyPerTick = 0; // No consumption while charging
                rechargingTimer -= tickDurationMs;
                if (rechargingTimer <= 0) {
                    setState(DroneState.ACTIVE);
                    autonomyRemaining = SimulationParams.DRONE_AUTONOMY_MS;
                }
                break;
        }
        
        // Update total energy consumption
        totalEnergyConsumed += energyPerTick;
    }
    
    // Start measuring at current position
    public void startMeasurement() {
        setState(DroneState.MEASURING);
        measurementTimer = SimulationParams.MEASUREMENT_DURATION_MS;
    }
    
    /**
     * Detect anomaly at current position and mark it as detected
     */
    public void detectAnomalyAt(Anomaly anomaly, long currentTime) {
        if (anomaly != null && !anomaly.isDetected()) {
            anomaly.markDetected(currentTime);
        }
    }
    
    // Set state
    public void setState(DroneState newState) {
        this.state = newState;
    }
    
    // Convenience: is at base?
    public boolean isAtBase() {
        return Math.sqrt(x * x + y * y) < 0.5;
    }
    
    // Measurement record
    public static class Measurement {
        public double intensity;
        public long timestamp;
        public double x, y;
        
        public Measurement(double intensity, long timestamp, double x, double y) {
            this.intensity = intensity;
            this.timestamp = timestamp;
            this.x = x;
            this.y = y;
        }
    }
}
