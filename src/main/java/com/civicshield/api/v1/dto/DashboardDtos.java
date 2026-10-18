package com.civicshield.api.v1.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class DashboardDtos {
    private DashboardDtos() {}

    public record AreaRisk(String zone, long openIncidents, int maxRiskScore, String maxRiskLevel) {}

    public record TopThreat(Long incidentId, String code, String location, String type,
                            int riskScore, String riskLevel, String explanation,
                            List<CascadeDtos.CascadeNode> nodes) {}

    public record DashboardSummary(
            LocalDateTime generatedAt,
            String dataProvenance,
            long activeIncidents,
            long highCriticalIncidents,
            long activeAlerts,
            long criticalActiveAlerts,
            long areasRequiringAttention,
            long pendingRecommendations,
            long totalIncidents,
            long simulationsRun,
            Double averageResponseMinutes,
            Map<String, Long> riskDistribution,
            List<AreaRisk> areas,
            TopThreat topThreat
    ) {}

    public record TrendPoint(String date, long incidents, long highRisk) {}

    public record TrendResponse(String from, String to, List<TrendPoint> points) {}

    public record ActivityItem(String type, LocalDateTime timestamp, String title,
                               String description, Long incidentId, String level) {}
}
