package com.civicshield.frontend.dto;

public record SimulationRequest(String scenarioName, String zone, double rainfall, double traffic, double drainage,
                                boolean pumps, boolean diversion, boolean greenCorridor, boolean hospitalSurge,
                                boolean save) {
}
