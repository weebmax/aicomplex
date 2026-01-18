package com.drones.model;

public class Anomaly {
    private double x, y;
    private double intensity; // 0 to 1
    private double maxIntensity;
    private long creationTime;
    private AnomalyType type;
    private long detectionTime = -1; // -1 = not detected yet
    private boolean detected = false;
    
    public Anomaly(double x, double y, double intensity, long creationTime) {
        this(x, y, intensity, creationTime, AnomalyType.POLLUTION);
    }
    
    public Anomaly(double x, double y, double intensity, long creationTime, AnomalyType type) {
        this.x = x;
        this.y = y;
        this.intensity = intensity;
        this.maxIntensity = intensity;
        this.creationTime = creationTime;
        this.type = type;
    }
    
    public double getX() { return x; }
    public double getY() { return y; }
    public double getIntensity() { return intensity; }
    public long getCreationTime() { return creationTime; }
    public double getMaxIntensity() { return maxIntensity; }
    public AnomalyType getType() { return type; }
    public boolean isDetected() { return detected; }
    public long getDetectionTime() { return detectionTime; }
    
    /**
     * Mark this anomaly as detected and record the detection time
     */
    public void markDetected(long detectionTime) {
        if (!this.detected) {
            this.detected = true;
            this.detectionTime = detectionTime;
        }
    }
    
    /**
     * Get detection latency in milliseconds (-1 if not detected)
     */
    public long getDetectionLatency() {
        return detected ? (detectionTime - creationTime) : -1;
    }
    
    public void setIntensity(double intensity) {
        this.intensity = Math.max(0, Math.min(1, intensity));
    }
    
    public void decay(double decayRate) {
        intensity *= decayRate;
    }
    
    /**
     * Decay using the type's decay rate
     */
    public void decayWithType() {
        intensity *= type.getDecayRate();
    }
    
    public boolean isAlive() {
        return intensity > 0.01;
    }
}
