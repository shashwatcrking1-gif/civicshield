package com.civicshield.frontend.dto;

public record SimulationParameter(String key, String label, String unit, double min, double max, double step) {
    public double clamp(double v) { return Math.max(min, Math.min(max, v)); }
}
