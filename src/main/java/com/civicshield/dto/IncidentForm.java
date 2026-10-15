package com.civicshield.dto;

import com.civicshield.model.IncidentType;
import com.civicshield.model.Severity;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Form backing object for "Report Incident". */
@Data
public class IncidentForm {

    public IncidentForm() {
    }

    @NotNull(message = "Select an incident type")
    private IncidentType incidentType;

    @NotBlank(message = "Location is required")
    @Size(min = 3, max = 200, message = "Location must be 3–200 characters")
    private String location;

    @NotNull(message = "Pick a point on the map or enter a latitude")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;

    @NotNull(message = "Pick a point on the map or enter a longitude")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;

    @NotNull(message = "Select a severity")
    private Severity severity;
}
