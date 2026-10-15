package com.civicshield.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Stored output of the cascade risk engine for one incident. */
@Entity
@Table(name = "risk_analyses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long riskId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @Column(nullable = false)
    private Double riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Column(length = 2000)
    private String predictedImpact;

    @Column(nullable = false)
    private LocalDateTime analysisDate;
}
