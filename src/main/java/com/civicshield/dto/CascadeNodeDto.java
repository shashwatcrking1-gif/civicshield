package com.civicshield.dto;

import com.civicshield.model.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One node of the cascade chain (e.g. Flood 81%). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}
