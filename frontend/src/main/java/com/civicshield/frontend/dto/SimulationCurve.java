package com.civicshield.frontend.dto;

import java.util.List;

public record SimulationCurve(List<Integer> rainfall, List<Integer> baseline, List<Integer> scenario) {
    public SimulationCurve {
        rainfall = rainfall == null ? List.of() : List.copyOf(rainfall);
        baseline = baseline == null ? List.of() : List.copyOf(baseline);
        scenario = scenario == null ? List.of() : List.copyOf(scenario);
    }
}
