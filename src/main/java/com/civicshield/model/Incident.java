package com.civicshield.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** A reported urban incident with its geo-location. */
@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long incidentId;

    @ManyToOne
    @JoinColumn(name = "reported_by")
    private User reportedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private IncidentType incidentType;

    @Column(nullable = false, length = 200)
    private String location;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentStatus status;

    @Column(nullable = false)
    private LocalDateTime reportedDate;

    @PrePersist
    void onCreate() {
        if (reportedDate == null) reportedDate = LocalDateTime.now();
        if (status == null) status = IncidentStatus.REPORTED;
    }

    /** Human friendly identifier, e.g. INC-0001. */
    public String getCode() {
        return incidentId == null ? "INC-NEW" : String.format("INC-%04d", incidentId);
    }
}
