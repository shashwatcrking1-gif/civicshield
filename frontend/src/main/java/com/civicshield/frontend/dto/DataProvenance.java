package com.civicshield.frontend.dto;

/**
 * Where the data on screen comes from. {@code DEMO} is produced only by the frontend's explicit demo mode
 * and never by the backend.
 */
public enum DataProvenance {
    OPERATIONAL("Operational data", "Records stored by the CivicShield backend."),
    SIMULATED("Simulated data", "Seeded demonstration records and simulated zone profiles – not live intelligence."),
    HYPOTHETICAL("Hypothetical scenario", "What-If output computed from assumptions you entered. Nothing was changed."),
    DEMO("Demo mode", "Illustrative records generated inside the frontend. The backend is not being used.");

    private final String label;
    private final String description;

    DataProvenance(String label, String description) { this.label = label; this.description = description; }

    public String label() { return label; }
    public String description() { return description; }

    /** True when the UI must show a prominent non-live warning. */
    public boolean requiresWarning() { return this != OPERATIONAL; }
}
