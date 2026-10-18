package com.civicshield.frontend.dto;

import java.util.List;
import java.util.Optional;

public record SimulationParameters(List<SimulationParameter> parameters, List<SimulationIntervention> interventions,
                                   List<ZoneDefault> zones) {
    public SimulationParameters {
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        interventions = interventions == null ? List.of() : List.copyOf(interventions);
        zones = zones == null ? List.of() : List.copyOf(zones);
    }

    public Optional<SimulationParameter> parameter(String key) {
        return parameters.stream().filter(p -> p.key().equals(key)).findFirst();
    }

    public boolean supportsIntervention(String key) {
        return interventions.stream().anyMatch(i -> i.key().equals(key));
    }
}
