package com.civicshield.api.v1.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public final class IncidentDtos {
    private IncidentDtos() {}

    public record RiskSnapshot(Long analysisId, int score, String level, LocalDateTime analysedAt) {
        public static RiskSnapshot from(com.civicshield.model.RiskAnalysis ra) {
            if (ra == null) return null;
            return new RiskSnapshot(ra.getRiskId(), (int) Math.round(ra.getRiskScore()), ra.getRiskLevel().name(), ra.getAnalysisDate());
        }
    }

    public record IncidentSummary(Long id, String code, String type, String location, String zone,
                                  Double latitude, Double longitude, String severity, String status,
                                  LocalDateTime reportedAt, String reportedBy, RiskSnapshot latestRisk) {
        public static IncidentSummary from(com.civicshield.model.Incident i, RiskSnapshot risk, String zone) {
            return new IncidentSummary(
                    i.getIncidentId(),
                    i.getCode(),
                    i.getIncidentType().name(),
                    i.getLocation(),
                    zone,
                    i.getLatitude(),
                    i.getLongitude(),
                    i.getSeverity().name(),
                    i.getStatus().name(),
                    i.getReportedDate(),
                    i.getReportedBy() != null ? i.getReportedBy().getName() : "Anonymous",
                    risk
            );
        }
    }

    public record IncidentDetail(Long id, String code, String type, String location, String zone,
                                 Double latitude, Double longitude, String severity, String status,
                                 LocalDateTime reportedAt, String reportedBy, RiskSnapshot latestRisk,
                                 String predictedImpact, int analysisCount) {}

    public record IncidentRequest(
            @NotBlank(message = "Type is required") String type,
            @NotBlank(message = "Location is required") @Size(min = 3, max = 200, message = "Location must be 3-200 characters") String location,
            @NotNull(message = "Latitude is required") @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90") @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90") Double latitude,
            @NotNull(message = "Longitude is required") @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180") @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180") Double longitude,
            @NotBlank(message = "Severity is required") String severity
    ) {}

    public record StatusUpdateRequest(@NotBlank(message = "Status is required") String status) {}

    public record MapIncident(Long id, String code, String type, String location, String zone,
                              Double latitude, Double longitude, String severity, String status,
                              Integer riskScore, String riskLevel, String chainSummary,
                              LocalDateTime reportedAt) {}
}
