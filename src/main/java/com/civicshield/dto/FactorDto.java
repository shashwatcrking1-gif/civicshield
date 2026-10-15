package com.civicshield.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Explainable-AI factor: how much an input contributed to the risk score. */
@Data
@Builder
public class FactorDto {
    private String key;
    private String name;
    /** Normalised factor value 0-100. */
    private int value;
    /** Share of the total risk contribution in percent (all factors sum to ~100). */
    private int contribution;
    private String description;

    public FactorDto() {
    }

    public FactorDto(String key, String name, int value, int contribution, String description) {
        this.key = key;
        this.name = name;
        this.value = value;
        this.contribution = contribution;
        this.description = description;
    }
}
