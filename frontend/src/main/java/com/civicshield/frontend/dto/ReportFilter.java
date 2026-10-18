package com.civicshield.frontend.dto;

import java.time.LocalDate;

/** Query filters shared by the report summary and CSV export. */
public record ReportFilter(LocalDate from, LocalDate to, IncidentType type, Level level, String q) {
    public static ReportFilter none() { return new ReportFilter(null, null, null, null, null); }
}
