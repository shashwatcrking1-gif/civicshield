package com.civicshield.frontend.dto;

import java.util.List;

public record RiskModel(RiskMethod method, List<ChainStep> chain, List<CascadeEdge> baseWeights) {
    public RiskModel {
        chain = chain == null ? List.of() : List.copyOf(chain);
        baseWeights = baseWeights == null ? List.of() : List.copyOf(baseWeights);
    }
}
