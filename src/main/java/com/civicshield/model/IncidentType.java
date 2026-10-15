package com.civicshield.model;

import lombok.Getter;

/** Supported incident types with an icon (Lucide name) and a flag for weather-driven events. */
@Getter
public enum IncidentType {
    HEAVY_RAIN("Heavy Rainfall", "cloud-rain", true),
    FLOOD("Flood", "waves", true),
    WATERLOGGING("Waterlogging", "droplets", true),
    DRAINAGE_OVERFLOW("Drainage Overflow", "waves", false),
    ROAD_BLOCKAGE("Road Blockage", "construction", false),
    TREE_FALL("Tree Fall", "tree-pine", false),
    ROAD_ACCIDENT("Road Accident", "car", false),
    TRAFFIC_CONGESTION("Traffic Congestion", "car", false),
    POWER_OUTAGE("Power Outage", "plug-zap", false),
    MEDICAL_EMERGENCY("Medical Emergency", "ambulance", false),
    FIRE("Fire", "flame", false);

    private final String label;
    private final String icon;
    private final boolean weatherDriven;

    IncidentType(String label, String icon, boolean weatherDriven) {
        this.label = label;
        this.icon = icon;
        this.weatherDriven = weatherDriven;
    }
}
