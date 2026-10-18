package com.civicshield.frontend.dto;

/** Portal roles defined by the backend. UI visibility is a convenience; the backend enforces access. */
public enum Role {
    ADMIN("Administrator"),
    AUTHORITY("Municipal Authority"),
    EMERGENCY_SERVICE("Emergency Service"),
    HOSPITAL("Hospital");

    private final String label;

    Role(String label) { this.label = label; }

    public String label() { return label; }
}
