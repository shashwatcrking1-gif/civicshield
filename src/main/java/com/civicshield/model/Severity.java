package com.civicshield.model;

import lombok.Getter;

/** Reported severity of an incident. {@code css} maps to the shared risk colour palette. */
@Getter
public enum Severity {
    LOW("Low", "low", 1),
    MEDIUM("Medium", "medium", 2),
    HIGH("High", "high", 3),
    CRITICAL("Critical", "critical", 4);

    private final String label;
    private final String css;
    private final int weight;

    Severity(String label, String css, int weight) {
        this.label = label;
        this.css = css;
        this.weight = weight;
    }
}
