package com.civicshield.dto;

import com.civicshield.model.RiskLevel;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Aggregated numbers for the portal dashboard. */
@Data
@Builder
public class DashboardStats {
    private long activeIncidents;
    private long highRiskZones;
    private long criticalAlerts;
    private long simulationsRun;
    private long totalIncidents;

    /** Risk level label → number of incidents (latest analysis). */
    @Builder.Default
    private Map<String, Long> riskDistribution = new LinkedHashMap<>();

    // "Top cascading threat"
    private Long topThreatIncidentId;
    private String topThreatCode;
    private String topThreatLocation;
    private String topThreatType;
    private Integer topThreatScore;
    private RiskLevel topThreatLevel;
    private String topThreatExplanation;
    @Builder.Default
    private List<CascadeNodeDto> topThreatNodes = new ArrayList<>();

    public DashboardStats() {
    }

    public DashboardStats(long activeIncidents, long highRiskZones, long criticalAlerts, long simulationsRun,
                          long totalIncidents, Map<String, Long> riskDistribution, Long topThreatIncidentId,
                          String topThreatCode, String topThreatLocation, String topThreatType,
                          Integer topThreatScore, RiskLevel topThreatLevel, String topThreatExplanation,
                          List<CascadeNodeDto> topThreatNodes) {
        this.activeIncidents = activeIncidents;
        this.highRiskZones = highRiskZones;
        this.criticalAlerts = criticalAlerts;
        this.simulationsRun = simulationsRun;
        this.totalIncidents = totalIncidents;
        this.riskDistribution = riskDistribution != null ? riskDistribution : new LinkedHashMap<>();
        this.topThreatIncidentId = topThreatIncidentId;
        this.topThreatCode = topThreatCode;
        this.topThreatLocation = topThreatLocation;
        this.topThreatType = topThreatType;
        this.topThreatScore = topThreatScore;
        this.topThreatLevel = topThreatLevel;
        this.topThreatExplanation = topThreatExplanation;
        this.topThreatNodes = topThreatNodes != null ? topThreatNodes : new ArrayList<>();
    }
}
