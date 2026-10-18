package com.civicshield.frontend.dto;

import java.time.LocalDateTime;

public record IncidentDetail(Long id, String code, IncidentType type, String location, String zone,
                             Double latitude, Double longitude, Level severity, IncidentStatus status,
                             LocalDateTime reportedAt, String reportedBy, RiskSnapshot latestRisk,
                             String predictedImpact, Integer analysisCount) {

    public IncidentSummary toSummary() {
        return new IncidentSummary(id, code, type, location, zone, latitude, longitude, severity, status,
                reportedAt, reportedBy, latestRisk);
    }
}
