package com.civicshield.frontend.dto;

import java.time.LocalDateTime;

public record ActivityItem(String type, LocalDateTime timestamp, String title, String description, Long incidentId,
                           Level level) {
}
