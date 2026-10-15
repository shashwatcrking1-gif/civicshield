package com.civicshield.dto;

import com.civicshield.model.Priority;
import com.civicshield.model.RiskLevel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Output of the cascade engine (Java) or the optional Python AI service. */
@Data
@Builder
public class SimulationResult {

    private int riskScore;
    private RiskLevel riskLevel;
    private String zone;
    /** JAVA_CASCADE_ENGINE or PYTHON_AI. */
    private String engine;

    @Builder.Default
    private List<CascadeNodeDto> nodes = new ArrayList<>();
    @Builder.Default
    private List<FactorDto> factors = new ArrayList<>();

    /** One-line explainable-AI summary. */
    private String explanation;
    /** Where intervening breaks the chain most effectively. */
    private String breakPoint;

    @Builder.Default
    private List<String> consequences = new ArrayList<>();
    @Builder.Default
    private List<ActionDto> actions = new ArrayList<>();

    public SimulationResult() {
    }

    public SimulationResult(int riskScore, RiskLevel riskLevel, String zone, String engine,
                            List<CascadeNodeDto> nodes, List<FactorDto> factors, String explanation,
                            String breakPoint, List<String> consequences, List<ActionDto> actions) {
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.zone = zone;
        this.engine = engine;
        this.nodes = nodes != null ? nodes : new ArrayList<>();
        this.factors = factors != null ? factors : new ArrayList<>();
        this.explanation = explanation;
        this.breakPoint = breakPoint;
        this.consequences = consequences != null ? consequences : new ArrayList<>();
        this.actions = actions != null ? actions : new ArrayList<>();
    }

    /** Finds a node by key (rain, flood, road, traffic, ambulance, hospital). */
    @JsonIgnore
    public CascadeNodeDto node(String key) {
        return nodes.stream().filter(n -> n.getKey().equals(key)).findFirst().orElse(null);
    }

    /** "Rain 92% → Flood 81% → ..." */
    @JsonIgnore
    public String chainSummary(int limit) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(limit, nodes.size()); i++) {
            if (i > 0) sb.append(" → ");
            sb.append(nodes.get(i).getLabel()).append(' ').append(nodes.get(i).getProbability()).append('%');
        }
        return sb.toString();
    }

    /** A recommended action with its priority and the chain node it targets. */
    @Data
    public static class ActionDto {
        private String text;
        private Priority priority;
        private String target;

        public ActionDto() {
        }

        public ActionDto(String text, Priority priority, String target) {
            this.text = text;
            this.priority = priority;
            this.target = target;
        }
    }
}
