package com.civicshield.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

/**
 * Input for the What-If simulator (public demo and portal).
 * Rainfall is in mm/hr; traffic and drainage capacity are percentages.
 * The intervention flags let authorities test how to "break the chain".
 */
@Data
@Builder
public class SimulationRequest {

    @Size(max = 120, message = "Scenario name must be at most 120 characters")
    private String scenarioName;

    /** Zone profile to use (Velachery, Adyar, T. Nagar, Guindy). */
    @Size(max = 40)
    private String zone;

    @NotNull(message = "Rainfall is required")
    @DecimalMin(value = "0", message = "Rainfall cannot be negative")
    @DecimalMax(value = "200", message = "Rainfall must be at most 200 mm/hr")
    private Double rainfall;

    @NotNull(message = "Traffic level is required")
    @DecimalMin("0") @DecimalMax("100")
    private Double traffic;

    @NotNull(message = "Drainage capacity is required")
    @DecimalMin("0") @DecimalMax("100")
    private Double drainage;

    /** Deploy mobile pumping units (+25 drainage capacity). */
    private boolean pumps;
    /** Activate traffic diversion (−20 traffic, weaker road→traffic link). */
    private boolean diversion;
    /** Open an ambulance green corridor (weaker traffic→ambulance link). */
    private boolean greenCorridor;
    /** Activate hospital surge protocol (weaker ambulance→hospital link). */
    private boolean hospitalSurge;

    /** Persist the scenario (portal only). */
    private boolean save;

    public SimulationRequest() {
    }

    public SimulationRequest(String scenarioName, String zone, Double rainfall, Double traffic,
                             Double drainage, boolean pumps, boolean diversion, boolean greenCorridor,
                             boolean hospitalSurge, boolean save) {
        this.scenarioName = scenarioName;
        this.zone = zone;
        this.rainfall = rainfall;
        this.traffic = traffic;
        this.drainage = drainage;
        this.pumps = pumps;
        this.diversion = diversion;
        this.greenCorridor = greenCorridor;
        this.hospitalSurge = hospitalSurge;
        this.save = save;
    }
}
