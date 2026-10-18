package com.civicshield.frontend.dto;

import java.util.List;

public record SimulationResponse(DataProvenance dataProvenance, Outcome baseline, Outcome scenario,
                                 SimulationCurve curve, List<String> interventions, List<String> assumptions,
                                 Long savedId) {
    public SimulationResponse {
        interventions = interventions == null ? List.of() : List.copyOf(interventions);
        assumptions = assumptions == null ? List.of() : List.copyOf(assumptions);
    }

    public int scoreDelta() { return scenario.riskScore() - baseline.riskScore(); }
}
