package com.civicshield.frontend.dto;

import java.time.LocalDate;
import java.util.List;

public record TrendResponse(LocalDate from, LocalDate to, List<TrendPoint> points) {
    public TrendResponse {
        points = points == null ? List.of() : List.copyOf(points);
    }
}
