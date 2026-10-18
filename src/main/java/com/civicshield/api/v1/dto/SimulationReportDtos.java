package com.civicshield.api.v1.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class SimulationReportDtos {
    private SimulationReportDtos() {}

    public record SimulationParametersDto(
            List<Map<String, Object>> parameters,
            List<Map<String, Object>> interventions,
            List<Map<String, Object>> zones
    ) {}

    public record SimulationRunResponse(
            String dataProvenance,
            CascadeDtos.Outcome baseline,
            CascadeDtos.Outcome scenario,
            Map<String, List<Integer>> curve,
            List<String> interventions,
            List<String> assumptions,
            Long savedId
    ) {}

    public record SimulationRecordDto(
            Long id,
            String scenarioName,
            Double rainfall,
            Double traffic,
            Double baselineRisk,
            String summary,
            String createdBy
    ) {}

    public record ReportSummaryDto(
            LocalDateTime generatedAt,
            String dataProvenance,
            long totalIncidents,
            Map<String, Long> byType,
            Map<String, Long> byLevel,
            Map<String, Long> byStatus,
            Map<String, Long> byZone,
            Map<String, Long> alerts,
            Map<String, Long> recommendations,
            long simulationsRun,
            List<IncidentDtos.IncidentSummary> rows
    ) {}
}
