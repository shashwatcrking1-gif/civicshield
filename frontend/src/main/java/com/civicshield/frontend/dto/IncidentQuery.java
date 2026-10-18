package com.civicshield.frontend.dto;

/** Filters for {@code GET /incidents}. Null values are omitted from the query string. */
public record IncidentQuery(String q, IncidentType type, Level severity, IncidentStatus status, String zone,
                            String sort) {
    public static IncidentQuery all() { return new IncidentQuery(null, null, null, null, null, null); }
}
