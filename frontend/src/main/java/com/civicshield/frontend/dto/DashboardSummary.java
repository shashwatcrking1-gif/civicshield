package com.civicshield.frontend.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record DashboardSummary(LocalDateTime generatedAt, DataProvenance dataProvenance, long activeIncidents,
                               long highCriticalIncidents, long activeAlerts, long criticalActiveAlerts,
                               long areasRequiringAttention, long pendingRecommendations, long totalIncidents,
                               long simulationsRun, Double averageResponseMinutes, Map<Level, Long> riskDistribution,
                               List<AreaRisk> areas, TopThreat topThreat) {
    public DashboardSummary {
        riskDistribution = riskDistribution == null ? Map.of() : Map.copyOf(riskDistribution);
        areas = areas == null ? List.of() : List.copyOf(areas);
    }
}
