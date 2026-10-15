package com.civicshield.model;

import lombok.Getter;

/** Normalised risk level derived from a 0-100 score. */
@Getter
public enum RiskLevel {
    LOW("Low", "low", "#22C55E"),
    MEDIUM("Medium", "medium", "#F59E0B"),
    HIGH("High", "high", "#F97316"),
    CRITICAL("Critical", "critical", "#EF4444");

    private final String label;
    private final String css;
    private final String color;

    RiskLevel(String label, String css, String color) {
        this.label = label;
        this.css = css;
        this.color = color;
    }

    /** Thresholds: Low &lt; 35 ≤ Medium &lt; 55 ≤ High &lt; 75 ≤ Critical. */
    public static RiskLevel fromScore(double score) {
        if (score >= 75) return CRITICAL;
        if (score >= 55) return HIGH;
        if (score >= 35) return MEDIUM;
        return LOW;
    }
}
