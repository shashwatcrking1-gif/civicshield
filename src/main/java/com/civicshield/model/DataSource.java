package com.civicshield.model;

import jakarta.persistence.*;
import lombok.*;

/** External data feed (weather, sensors, traffic, hospitals...). */
@Entity
@Table(name = "data_sources")
@Getter
@Setter
@Builder
public class DataSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sourceId;

    @Column(nullable = false, length = 150)
    private String sourceName;

    @Column(nullable = false, length = 60)
    private String sourceType;

    @Column(length = 150)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DataStatus dataStatus;

    public DataSource() {
    }

    public DataSource(Long sourceId, String sourceName, String sourceType, String location, DataStatus dataStatus) {
        this.sourceId = sourceId;
        this.sourceName = sourceName;
        this.sourceType = sourceType;
        this.location = location;
        this.dataStatus = dataStatus;
    }
}
