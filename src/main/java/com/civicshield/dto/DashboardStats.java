package com.civicshield.dto;

import com.civicshield.model.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Aggregated numbers for the portal dashboard. */
@Data
@NoArgsConstructor
@AllArgsConstructor
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
}
