package com.civicshield.frontend.dto;

public record Zone(String key, String name, double latitude, double longitude, int drainage, int roadVulnerability,
                   int historical, int hospitalLoad, double exposure, int baseTraffic, String diversionRoute,
                   String hospital, boolean simulatedProfile) {
}
