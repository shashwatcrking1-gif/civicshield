package com.civicshield.frontend.dto;

public enum IncidentStatus {
    REPORTED("Reported", "info"),
    VERIFIED("Verified", "indigo"),
    IN_PROGRESS("In progress", "warn"),
    RESOLVED("Resolved", "ok");

    private final String label;
    private final String tone;

    IncidentStatus(String label, String tone) { this.label = label; this.tone = tone; }

    public String label() { return label; }
    public String tone() { return tone; }
}
