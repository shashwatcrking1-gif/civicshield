package com.civicshield.frontend.dto;

import java.util.List;

public record TopThreat(Long incidentId, String code, String location, IncidentType type, Integer riskScore,
                        Level riskLevel, String explanation, List<CascadeNode> nodes) {
    public TopThreat {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
    }
}
