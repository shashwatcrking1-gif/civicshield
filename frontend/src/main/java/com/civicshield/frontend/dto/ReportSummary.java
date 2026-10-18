package com.civicshield.frontend.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ReportSummary(LocalDateTime generatedAt, DataProvenance dataProvenance, long totalIncidents,
                            Map<IncidentType, Long> byType, Map<Level, Long> byLevel,
                            Map<IncidentStatus, Long> byStatus, Map<String, Long> byZone,
                            Map<AlertStatus, Long> alerts, Map<RecommendationStatus, Long> recommendations,
                            long simulationsRun, List<IncidentSummary> rows) {
    public ReportSummary {
        byType = byType == null ? Map.of() : Map.copyOf(byType);
        byLevel = byLevel == null ? Map.of() : Map.copyOf(byLevel);
        byStatus = byStatus == null ? Map.of() : Map.copyOf(byStatus);
        byZone = byZone == null ? Map.of() : Map.copyOf(byZone);
        alerts = alerts == null ? Map.of() : Map.copyOf(alerts);
        recommendations = recommendations == null ? Map.of() : Map.copyOf(recommendations);
        rows = rows == null ? List.of() : List.copyOf(rows);
    }
}
