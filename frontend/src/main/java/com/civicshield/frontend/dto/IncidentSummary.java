package com.civicshield.frontend.dto;

import java.time.LocalDateTime;

public record IncidentSummary(Long id, String code, IncidentType type, String location, String zone,
                              Double latitude, Double longitude, Level severity, IncidentStatus status,
                              LocalDateTime reportedAt, String reportedBy, RiskSnapshot latestRisk) {
    public boolean isOpen() { return status != IncidentStatus.RESOLVED; }
}
