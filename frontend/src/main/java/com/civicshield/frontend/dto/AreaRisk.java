package com.civicshield.frontend.dto;

public record AreaRisk(String zone, int openIncidents, Integer maxRiskScore, Level maxRiskLevel) {
}
