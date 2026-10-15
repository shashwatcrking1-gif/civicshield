package com.civicshield.model;

import jakarta.persistence.*;
import lombok.*;

/** External data feed (weather, sensors, traffic, hospitals...). */
@Entity
@Table(name = "data_sources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
}
