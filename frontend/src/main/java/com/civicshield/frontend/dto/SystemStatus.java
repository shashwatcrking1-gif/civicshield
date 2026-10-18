package com.civicshield.frontend.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record SystemStatus(String status, String application, String version, String apiVersion,
                           LocalDateTime serverTime, String timezone, DataProvenance dataProvenance,
                           String riskEngine, boolean aiServiceEnabled, Map<DataStatus, Long> dataSources) {
    public SystemStatus {
        dataSources = dataSources == null ? Map.of() : Map.copyOf(dataSources);
    }

    public boolean isUp() { return "UP".equalsIgnoreCase(status); }
}
