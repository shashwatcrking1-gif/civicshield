package com.civicshield.api.v1.controller;

import com.civicshield.api.v1.dto.IncidentDtos.IncidentSummary;
import com.civicshield.api.v1.dto.IncidentDtos.RiskSnapshot;
import com.civicshield.api.v1.dto.SimulationReportDtos.ReportSummaryDto;
import com.civicshield.model.Incident;
import com.civicshield.model.IncidentType;
import com.civicshield.model.RiskAnalysis;
import com.civicshield.model.RiskLevel;
import com.civicshield.service.AlertService;
import com.civicshield.service.CascadeRiskEngine;
import com.civicshield.service.RecommendationService;
import com.civicshield.service.ReportService;
import com.civicshield.service.ReportService.ReportRow;
import com.civicshield.service.SimulationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportApiController {

    private final ReportService reportService;
    private final AlertService alertService;
    private final RecommendationService recommendationService;
    private final SimulationService simulationService;
    private final CascadeRiskEngine cascadeRiskEngine;

    public ReportApiController(ReportService reportService,
                               AlertService alertService,
                               RecommendationService recommendationService,
                               SimulationService simulationService,
                               CascadeRiskEngine cascadeRiskEngine) {
        this.reportService = reportService;
        this.alertService = alertService;
        this.recommendationService = recommendationService;
        this.simulationService = simulationService;
        this.cascadeRiskEngine = cascadeRiskEngine;
    }

    @GetMapping("/summary")
    public ResponseEntity<ReportSummaryDto> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) IncidentType type,
            @RequestParam(required = false) RiskLevel level,
            @RequestParam(required = false) String q) {

        List<ReportRow> rows = reportService.rows(from, to, type, level, q);

        Map<String, Long> byType = new LinkedHashMap<>();
        for (IncidentType t : IncidentType.values()) byType.put(t.name(), 0L);
        rows.forEach(r -> byType.merge(r.getIncident().getIncidentType().name(), 1L, Long::sum));

        Map<String, Long> byLevel = new LinkedHashMap<>();
        for (RiskLevel l : RiskLevel.values()) byLevel.put(l.name(), 0L);
        rows.stream().filter(r -> r.getAnalysis() != null)
                .forEach(r -> byLevel.merge(r.getAnalysis().getRiskLevel().name(), 1L, Long::sum));

        Map<String, Long> byStatus = new LinkedHashMap<>();
        rows.forEach(r -> byStatus.merge(r.getIncident().getStatus().name(), 1L, Long::sum));

        Map<String, Long> byZone = new LinkedHashMap<>();
        rows.forEach(r -> {
            String z = cascadeRiskEngine.zoneForLocation(r.getIncident().getLocation()).name();
            byZone.merge(z, 1L, Long::sum);
        });

        Map<String, Long> alertCounts = new LinkedHashMap<>();
        alertService.counts().forEach((k, v) -> alertCounts.put(k.name(), v));

        Map<String, Long> recCounts = new LinkedHashMap<>();
        recommendationService.counts().forEach((k, v) -> recCounts.put(k.name(), v));

        List<IncidentSummary> summaryRows = rows.stream().map(r -> {
            Incident inc = r.getIncident();
            RiskAnalysis ra = r.getAnalysis();
            String zone = cascadeRiskEngine.zoneForLocation(inc.getLocation()).name();
            return IncidentSummary.from(inc, RiskSnapshot.from(ra), zone);
        }).toList();

        return ResponseEntity.ok(new ReportSummaryDto(
                LocalDateTime.now(),
                "SIMULATED",
                rows.size(),
                byType,
                byLevel,
                byStatus,
                byZone,
                alertCounts,
                recCounts,
                simulationService.count(),
                summaryRows
        ));
    }

    @GetMapping(value = "/incidents.csv", produces = "text/csv")
    public ResponseEntity<String> exportCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) IncidentType type,
            @RequestParam(required = false) RiskLevel level,
            @RequestParam(required = false) String q) {

        List<ReportRow> rows = reportService.rows(from, to, type, level, q);
        String csv = reportService.toCsv(rows);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"civicshield-incidents-" + LocalDate.now() + ".csv\"");

        return ResponseEntity.ok().headers(headers).body(csv);
    }
}
