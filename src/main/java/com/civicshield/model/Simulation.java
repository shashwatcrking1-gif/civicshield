package com.civicshield.model;

import jakarta.persistence.*;
import lombok.*;

/** A saved What-If scenario run by an authority user. */
@Entity
@Table(name = "simulations")
@Getter
@Setter
public class Simulation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long simulationId;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false, length = 120)
    private String scenarioName;

    @Column(nullable = false)
    private Double rainfallLevel;

    @Column(nullable = false)
    private Double trafficLevel;

    @Column(nullable = false)
    private Double predictedRisk;

    @Column(length = 1000)
    private String result;

    public Simulation() {
    }

    @Builder
    public Simulation(Long simulationId, User createdBy, String scenarioName, Double rainfallLevel,
                      Double trafficLevel, Double predictedRisk, String result) {
        this.simulationId = simulationId;
        this.createdBy = createdBy;
        this.scenarioName = scenarioName;
        this.rainfallLevel = rainfallLevel;
        this.trafficLevel = trafficLevel;
        this.predictedRisk = predictedRisk;
        this.result = result;
    }
}
