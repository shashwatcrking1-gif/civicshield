package com.civicshield.frontend.dto;

import java.time.LocalDateTime;

public record MapIncident(Long id, String code, IncidentType type, String location, String zone, Double latitude,
                          Double longitude, Level severity, IncidentStatus status, Integer riskScore,
                          Level riskLevel, String chainSummary, LocalDateTime reportedAt) {
    public boolean hasValidCoordinates() {
        return latitude != null && longitude != null && latitude >= -90 && latitude <= 90
                && longitude >= -180 && longitude <= 180;
    }
}
