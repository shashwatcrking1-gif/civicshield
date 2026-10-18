package com.civicshield.api.v1.dto;

import java.time.LocalDateTime;

public final class AlertRecommendationDtos {
    private AlertRecommendationDtos() {}

    public record AlertDto(Long id, String message, String priority, String status, LocalDateTime createdAt,
                           String source, Long incidentId, String incidentCode, String incidentType,
                           String location, String zone, Integer riskScore, String riskLevel) {

        public static AlertDto from(com.civicshield.model.Alert a, String zone) {
            com.civicshield.model.RiskAnalysis ra = a.getRiskAnalysis();
            com.civicshield.model.Incident inc = ra != null ? ra.getIncident() : null;
            return new AlertDto(
                    a.getAlertId(),
                    a.getMessage(),
                    a.getPriority().name(),
                    a.getAlertStatus().name(),
                    a.getAlertDate(),
                    "CASCADE_RISK_ENGINE",
                    inc != null ? inc.getIncidentId() : null,
                    inc != null ? inc.getCode() : null,
                    inc != null ? inc.getIncidentType().name() : null,
                    inc != null ? inc.getLocation() : null,
                    zone,
                    ra != null ? (int) Math.round(ra.getRiskScore()) : null,
                    ra != null && ra.getRiskLevel() != null ? ra.getRiskLevel().name() : null
            );
        }
    }

    public record RecommendationDto(Long id, String action, String priority, String status,
                                    Long incidentId, String incidentCode, String incidentType,
                                    String location, String zone, Long analysisId, Integer riskScore,
                                    String riskLevel, LocalDateTime analysedAt, String rationale) {

        public static RecommendationDto from(com.civicshield.model.Recommendation r, String zone) {
            com.civicshield.model.RiskAnalysis ra = r.getRiskAnalysis();
            com.civicshield.model.Incident inc = ra != null ? ra.getIncident() : null;
            return new RecommendationDto(
                    r.getRecommendationId(),
                    r.getAction(),
                    r.getPriority().name(),
                    r.getStatus().name(),
                    inc != null ? inc.getIncidentId() : null,
                    inc != null ? inc.getCode() : null,
                    inc != null ? inc.getIncidentType().name() : null,
                    inc != null ? inc.getLocation() : null,
                    zone,
                    ra != null ? ra.getRiskId() : null,
                    ra != null ? (int) Math.round(ra.getRiskScore()) : null,
                    ra != null && ra.getRiskLevel() != null ? ra.getRiskLevel().name() : null,
                    ra != null ? ra.getAnalysisDate() : null,
                    ra != null ? ra.getPredictedImpact() : null
            );
        }
    }
}
