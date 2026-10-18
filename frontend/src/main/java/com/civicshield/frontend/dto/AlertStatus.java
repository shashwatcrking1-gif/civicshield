package com.civicshield.frontend.dto;

public enum AlertStatus {
    ACTIVE("Active", "danger"),
    ACKNOWLEDGED("Acknowledged", "warn"),
    RESOLVED("Resolved", "ok");

    private final String label;
    private final String tone;

    AlertStatus(String label, String tone) { this.label = label; this.tone = tone; }

    public String label() { return label; }
    public String tone() { return tone; }
}
