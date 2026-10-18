package com.civicshield.frontend.dto;

import java.util.Map;

/** How a score was computed ({@code Method} in docs/API.md). */
public record RiskMethod(String propagation, String nodeRisk, String score, double damping,
                         Map<String, Double> localFactorCoefficients, double rainFactor,
                         Map<Level, Integer> thresholds, boolean scoreIsCalibratedProbability) {
    public RiskMethod {
        localFactorCoefficients = localFactorCoefficients == null ? Map.of() : Map.copyOf(localFactorCoefficients);
        thresholds = thresholds == null ? Map.of() : Map.copyOf(thresholds);
    }
}
