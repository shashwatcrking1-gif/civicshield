package com.civicshield.frontend.dto;

public enum IncidentType {
    HEAVY_RAIN("Heavy Rainfall"),
    FLOOD("Flood"),
    WATERLOGGING("Waterlogging"),
    DRAINAGE_OVERFLOW("Drainage Overflow"),
    ROAD_BLOCKAGE("Road Blockage"),
    TREE_FALL("Tree Fall"),
    ROAD_ACCIDENT("Road Accident"),
    TRAFFIC_CONGESTION("Traffic Congestion"),
    POWER_OUTAGE("Power Outage"),
    MEDICAL_EMERGENCY("Medical Emergency"),
    FIRE("Fire");

    private final String label;

    IncidentType(String label) { this.label = label; }

    public String label() { return label; }
}
