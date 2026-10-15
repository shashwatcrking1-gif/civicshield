package com.civicshield.model;

import lombok.Getter;

/** Lifecycle of an alert: Active → Acknowledged → Resolved. */
@Getter
public enum AlertStatus {
    ACTIVE("Active", "danger"),
    ACKNOWLEDGED("Acknowledged", "warn"),
    RESOLVED("Resolved", "ok");

    private final String label;
    private final String css;

    AlertStatus(String label, String css) {
        this.label = label;
        this.css = css;
    }
}
