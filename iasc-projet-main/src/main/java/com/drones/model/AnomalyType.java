package com.drones.model;

/**
 * Enum representing different types of anomalies with distinct behaviors
 */
public enum AnomalyType {
    POLLUTION {
        @Override
        public double getDecayRate() {
            return 0.90; // Slow decay
        }

        @Override
        public double getDiffusionFactor() {
            return 0.15; // High diffusion
        }

        @Override
        public String getDisplayName() {
            return "Pollution";
        }
    },
    
    STRUCTURAL_FAILURE {
        @Override
        public double getDecayRate() {
            return 0.98; // Very slow decay
        }

        @Override
        public double getDiffusionFactor() {
            return 0.05; // Low diffusion
        }

        @Override
        public String getDisplayName() {
            return "Structural Failure";
        }
    },
    
    RADIATION {
        @Override
        public double getDecayRate() {
            return 0.85; // Fast decay
        }

        @Override
        public double getDiffusionFactor() {
            return 0.10; // Medium diffusion
        }

        @Override
        public String getDisplayName() {
            return "Radiation";
        }
    };

    /**
     * Get the decay rate for this anomaly type (0-1, where 1 = no decay)
     */
    public abstract double getDecayRate();

    /**
     * Get the diffusion factor for this anomaly type
     */
    public abstract double getDiffusionFactor();

    /**
     * Get display name for UI
     */
    public abstract String getDisplayName();
}
