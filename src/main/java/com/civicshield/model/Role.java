package com.civicshield.model;

import lombok.Getter;

/** Portal roles. Spring Security uses the names with a ROLE_ prefix. */
@Getter
public enum Role {
    ADMIN("Administrator", "Full access, including user management"),
    AUTHORITY("Municipal Authority", "Incidents, risk, alerts, recommendations, simulator and reports"),
    EMERGENCY_SERVICE("Emergency Service", "Map, incidents (read), alerts and recommendations"),
    HOSPITAL("Hospital", "Read-only dashboard, map and alerts");

    private final String label;
    private final String description;

    Role(String label, String description) {
        this.label = label;
        this.description = description;
    }
}
