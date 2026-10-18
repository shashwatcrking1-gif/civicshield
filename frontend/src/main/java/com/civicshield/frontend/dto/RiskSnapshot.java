package com.civicshield.frontend.dto;

import java.time.LocalDateTime;

public record RiskSnapshot(Long analysisId, Integer score, Level level, LocalDateTime analysedAt) {
}
