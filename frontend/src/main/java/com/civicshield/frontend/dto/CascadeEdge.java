package com.civicshield.frontend.dto;

/** Propagation link between two nodes; {@code effectiveWeight} includes intervention modifiers. */
public record CascadeEdge(String from, String to, double baseWeight, double effectiveWeight, String description) {
    public boolean isModified() { return Math.abs(baseWeight - effectiveWeight) > 1e-9; }
}
