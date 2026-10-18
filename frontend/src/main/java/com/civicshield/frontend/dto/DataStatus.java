package com.civicshield.frontend.dto;

public enum DataStatus {
    ONLINE("Online", "ok"),
    DELAYED("Delayed", "warn"),
    OFFLINE("Offline", "danger");

    private final String label;
    private final String tone;

    DataStatus(String label, String tone) { this.label = label; this.tone = tone; }

    public String label() { return label; }
    public String tone() { return tone; }
}
