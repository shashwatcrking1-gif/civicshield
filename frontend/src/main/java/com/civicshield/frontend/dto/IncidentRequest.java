package com.civicshield.frontend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body for {@code POST /incidents} and {@code PUT /incidents/{id}}. The same constraints are enforced by the
 * backend; they are repeated here so operators get immediate feedback.
 */
public class IncidentRequest {
    @NotNull(message = "Select an incident category")
    private IncidentType type;

    @NotBlank(message = "Location is required")
    @Size(min = 3, max = 200, message = "Location must be 3–200 characters")
    private String location;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;

    @NotNull(message = "Select a severity")
    private Level severity;

    public IncidentRequest() {
    }

    public static IncidentRequest from(IncidentDetail d) {
        IncidentRequest r = new IncidentRequest();
        r.type = d.type();
        r.location = d.location();
        r.latitude = d.latitude();
        r.longitude = d.longitude();
        r.severity = d.severity();
        return r;
    }

    public IncidentType getType() { return type; }
    public void setType(IncidentType type) { this.type = type; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location == null ? null : location.trim(); }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Level getSeverity() { return severity; }
    public void setSeverity(Level severity) { this.severity = severity; }
}
