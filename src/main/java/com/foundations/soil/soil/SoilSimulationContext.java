package com.foundations.soil.soil;

public record SoilSimulationContext(double temperatureC, double hydration) {
    public SoilSimulationContext {
        hydration = Math.max(0.0, Math.min(1.0, hydration));
    }
}
