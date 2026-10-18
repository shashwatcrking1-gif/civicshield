package com.civicshield.frontend.dto;

public enum RecommendationStatus {
    PENDING("Pending review", "info"),
    APPROVED("Approved", "ok"),
    DISMISSED("Dismissed", "muted");

    private final String label;
    private final String tone;

    RecommendationStatus(String label, String tone) { this.label = label; this.tone = tone; }

    public String label() { return label; }
    public String tone() { return tone; }
}
