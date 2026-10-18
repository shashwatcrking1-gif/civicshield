package com.civicshield.frontend.dto;

public record EngineInputs(String zone, double rainfall, double drainage, double traffic, double roadVulnerability,
                           double historical, double hospitalLoad, double exposure, boolean pumps,
                           boolean diversion, boolean greenCorridor, boolean hospitalSurge) {
}
