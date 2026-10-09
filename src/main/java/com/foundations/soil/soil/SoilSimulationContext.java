package com.foundations.soil.soil;

public record SoilSimulationContext(double temperatureC, double hydration) {
    public SoilSimulationContext {
        if (!Double.isFinite(temperatureC) || !Double.isFinite(hydration))
            throw new IllegalArgumentException("Climate inputs must be finite");
        hydration = Math.max(0.0, Math.min(1.0, hydration));
    }
}
