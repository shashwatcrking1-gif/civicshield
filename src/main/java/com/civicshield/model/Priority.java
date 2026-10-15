package com.civicshield.model;

import lombok.Getter;

/** Priority for alerts and recommendations. */
@Getter
public enum Priority {
    LOW("Low", "low"),
    MEDIUM("Medium", "medium"),
    HIGH("High", "high"),
    CRITICAL("Critical", "critical");

    private final String label;
    private final String css;

    Priority(String label, String css) {
        this.label = label;
        this.css = css;
    }

    /** Maps a cascade-node probability (0-100) to an action priority. */
    public static Priority fromProbability(double p) {
        if (p >= 75) return CRITICAL;
        if (p >= 55) return HIGH;
        if (p >= 35) return MEDIUM;
        return LOW;
    }

    public static Priority fromRiskLevel(RiskLevel level) {
        return switch (level) {
            case CRITICAL -> CRITICAL;
            case HIGH -> HIGH;
            case MEDIUM -> MEDIUM;
            default -> LOW;
        };
    }
}
