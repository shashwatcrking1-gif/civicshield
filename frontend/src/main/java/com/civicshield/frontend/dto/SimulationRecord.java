package com.civicshield.frontend.dto;

public record SimulationRecord(Long id, String scenarioName, Double rainfall, Double traffic, Double baselineRisk,
                               String summary, String createdBy) {
}
