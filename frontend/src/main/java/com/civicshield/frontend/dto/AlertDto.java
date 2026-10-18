package com.civicshield.frontend.dto;

import java.time.LocalDateTime;

public record AlertDto(Long id, String message, Level priority, AlertStatus status, LocalDateTime createdAt,
                       String source, Long incidentId, String incidentCode, IncidentType incidentType,
                       String location, String zone, Integer riskScore, Level riskLevel) {
}
