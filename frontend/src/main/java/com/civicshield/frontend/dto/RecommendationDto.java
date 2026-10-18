package com.civicshield.frontend.dto;

import java.time.LocalDateTime;

public record RecommendationDto(Long id, String action, Level priority, RecommendationStatus status, Long incidentId,
                                String incidentCode, IncidentType incidentType, String location, String zone,
                                Long analysisId, Integer riskScore, Level riskLevel, LocalDateTime analysedAt,
                                String rationale) {
}
