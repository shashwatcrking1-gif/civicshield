package com.civicshield.frontend.dto;

import java.util.List;
import java.util.Optional;

/** Full cascade-engine result. */
public record Outcome(int riskScore, Level riskLevel, String zone, String engine, String explanation,
                      String breakPoint, List<CascadeNode> nodes, List<CascadeEdge> edges, List<Factor> factors,
                      List<MitigationAction> actions, List<String> consequences, EngineInputs inputs) {
    public Outcome {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        edges = edges == null ? List.of() : List.copyOf(edges);
        factors = factors == null ? List.of() : List.copyOf(factors);
        actions = actions == null ? List.of() : List.copyOf(actions);
        consequences = consequences == null ? List.of() : List.copyOf(consequences);
    }

    public Optional<CascadeNode> node(String key) {
        return nodes.stream().filter(n -> n.key().equals(key)).findFirst();
    }

    public Optional<CascadeEdge> edgeInto(String key) {
        return edges.stream().filter(e -> e.to().equals(key)).findFirst();
    }

    public Optional<CascadeEdge> edgeFrom(String key) {
        return edges.stream().filter(e -> e.from().equals(key)).findFirst();
    }

    public List<MitigationAction> actionsFor(String nodeLabel) {
        return actions.stream().filter(a -> nodeLabel.equalsIgnoreCase(a.target())).toList();
    }
}
