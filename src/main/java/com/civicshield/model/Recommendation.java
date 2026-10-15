package com.civicshield.model;

import jakarta.persistence.*;
import lombok.*;

/** AI-suggested action. Authorities approve or dismiss – the system never acts on its own. */
@Entity
@Table(name = "recommendations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recommendationId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "risk_id")
    private RiskAnalysis riskAnalysis;

    @Column(nullable = false, length = 500)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecommendationStatus status;
}
