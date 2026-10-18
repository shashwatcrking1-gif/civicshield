package com.civicshield.frontend.dto;

import java.time.LocalDate;

public record TrendPoint(LocalDate date, long incidents, long highRisk) {
}
