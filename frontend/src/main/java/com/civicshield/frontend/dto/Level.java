package com.civicshield.frontend.dto;

/**
 * Shared four-step scale used by the backend for {@code Severity}, {@code Priority} and {@code RiskLevel}.
 * The label is always rendered next to the colour so meaning never relies on colour alone.
 */
public enum Level {
    LOW("Low", "low", 1),
    MEDIUM("Medium", "medium", 2),
    HIGH("High", "high", 3),
    CRITICAL("Critical", "critical", 4);

    private final String label;
    private final String cssKey;
    private final int rank;

    Level(String label, String cssKey, int rank) {
        this.label = label;
        this.cssKey = cssKey;
        this.rank = rank;
    }

    public String label() { return label; }
    public String cssKey() { return cssKey; }
    public int rank() { return rank; }

    /** Mirrors the backend thresholds: Low &lt; 35 ≤ Medium &lt; 55 ≤ High &lt; 75 ≤ Critical. */
    public static Level fromScore(double score) {
        if (score >= 75) return CRITICAL;
        if (score >= 55) return HIGH;
        if (score >= 35) return MEDIUM;
        return LOW;
    }
}
