package com.civicshield.model;

import lombok.Getter;

/** Lifecycle of an incident; the order drives the status timeline on the detail page. */
@Getter
public enum IncidentStatus {
    REPORTED("Reported", "info", "Incident logged by field staff or citizen feed"),
    VERIFIED("Verified", "indigo", "Confirmed by the control room"),
    IN_PROGRESS("In Progress", "warn", "Response teams deployed"),
    RESOLVED("Resolved", "ok", "Situation normalised and closed");

    private final String label;
    private final String css;
    private final String description;

    IncidentStatus(String label, String css, String description) {
        this.label = label;
        this.css = css;
        this.description = description;
    }
}
