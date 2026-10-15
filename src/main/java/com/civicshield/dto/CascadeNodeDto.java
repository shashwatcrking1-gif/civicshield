package com.civicshield.dto;

import com.civicshield.model.RiskLevel;
import lombok.Builder;
import lombok.Data;

/** One node of the cascade chain (e.g. Flood 81%). */
@Data
public class CascadeNodeDto {
    private String key;
    private String label;
    /** Lucide icon name. */
    private String icon;
    /** Probability 0-100. */
    private int probability;
    /** Impact severity weight 0-1. */
    private double severity;
    private RiskLevel level;
    private String consequence;

    public CascadeNodeDto() {
    }

    @Builder
    public CascadeNodeDto(String key, String label, String icon, int probability,
                          double severity, RiskLevel level, String consequence) {
        this.key = key;
        this.label = label;
        this.icon = icon;
        this.probability = probability;
        this.severity = severity;
        this.level = level;
        this.consequence = consequence;
    }
}
