package com.civicshield.api.v1.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class CascadeDtos {
    private CascadeDtos() {}

    public record CascadeNode(String key, String label, String icon, int probability, double severity,
                              String level, String consequence, String upstream, String downstream) {}

    public record CascadeEdge(String from, String to, double baseWeight, double effectiveWeight, String description) {}

    public record Factor(String key, String name, int value, int contribution, String description) {}

    public record Action(String text, String priority, String target) {}

    public record EngineInputs(String zone, double rainfall, double drainage, double traffic,
                               double roadVulnerability, double historical, double hospitalLoad,
                               double exposure, boolean pumps, boolean diversion,
                               boolean greenCorridor, boolean hospitalSurge) {}

    public record Method(String propagation, String nodeRisk, String score, double damping,
                         Map<String, Double> localFactorCoefficients, double rainFactor,
                         Map<String, Integer> thresholds, boolean scoreIsCalibratedProbability) {

        public static Method standard() {
            return new Method(
                    "next = previous × effectiveWeight + localFactor × k",
                    "probability × severity × exposure",
                    "100 × (1 − Π(1 − damping × nodeRisk))",
                    0.55,
                    Map.of("drainage", 0.25, "road", 0.20, "traffic", 0.30, "history", 0.20, "hospital", 0.20),
                    0.80,
                    Map.of("MEDIUM", 35, "HIGH", 55, "CRITICAL", 75),
                    false
            );
        }
    }

    public record Outcome(int riskScore, String riskLevel, String zone, String engine,
                          String explanation, String breakPoint, List<CascadeNode> nodes,
                          List<CascadeEdge> edges, List<Factor> factors, List<Action> actions,
                          List<String> consequences, EngineInputs inputs) {}

    public record IncidentCascadeResponse(Long incidentId, String incidentCode, String location,
                                          Long analysisId, LocalDateTime analysedAt,
                                          Integer storedRiskScore, LocalDateTime evaluatedAt,
                                          String dataProvenance, Outcome outcome, Method method) {}

    public record RiskModelResponse(Method method, List<Map<String, Object>> chain, List<CascadeEdge> baseWeights) {}

    public record ZoneDto(String key, String name, double latitude, double longitude, int drainage,
                          int roadVulnerability, int historical, int hospitalLoad, double exposure,
                          int baseTraffic, String diversionRoute, String hospital, boolean simulatedProfile) {}
}
