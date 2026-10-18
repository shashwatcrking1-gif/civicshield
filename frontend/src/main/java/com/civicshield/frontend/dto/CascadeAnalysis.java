package com.civicshield.frontend.dto;

import java.time.LocalDateTime;

public record CascadeAnalysis(Long incidentId, String incidentCode, String location, Long analysisId,
                              LocalDateTime analysedAt, Integer storedRiskScore, LocalDateTime evaluatedAt,
                              DataProvenance dataProvenance, Outcome outcome, RiskMethod method) {
}
