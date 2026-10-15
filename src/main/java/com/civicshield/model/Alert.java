package com.civicshield.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Alert raised from a risk analysis. */
@Entity
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long alertId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "risk_id")
    private RiskAnalysis riskAnalysis;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertStatus alertStatus;

    @Column(nullable = false)
    private LocalDateTime alertDate;
}
