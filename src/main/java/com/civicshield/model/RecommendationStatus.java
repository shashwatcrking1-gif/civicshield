package com.civicshield.model;

import lombok.Getter;

/** Human decision on an AI recommendation. Final decisions always stay with authorities. */
@Getter
public enum RecommendationStatus {
    PENDING("Pending review", "info"),
    APPROVED("Approved", "ok"),
    DISMISSED("Dismissed", "muted");

    private final String label;
    private final String css;

    RecommendationStatus(String label, String css) {
        this.label = label;
        this.css = css;
    }
}
