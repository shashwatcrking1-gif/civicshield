package com.civicshield.frontend.dto;

/** One step of the cascade chain. {@code probability} is the engine's 0–100 node likelihood index. */
public record CascadeNode(String key, String label, String icon, int probability, double severity, Level level,
                          String consequence, String upstream, String downstream) {
}
