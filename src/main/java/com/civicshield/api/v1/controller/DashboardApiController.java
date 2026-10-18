package com.civicshield.api.v1.controller;

import com.civicshield.api.v1.dto.CascadeDtos.CascadeNode;
import com.civicshield.api.v1.dto.DashboardDtos.*;
import com.civicshield.dto.CascadeNodeDto;
import com.civicshield.dto.SimulationResult;
import com.civicshield.model.*;
import com.civicshield.repository.AlertRepository;
import com.civicshield.repository.IncidentRepository;
import com.civicshield.repository.RecommendationRepository;
import com.civicshield.repository.SimulationRepository;
import com.civicshield.service.CascadeRiskEngine;
import com.civicshield.service.RiskAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardApiController {

    private final IncidentRepository incidentRepository;
    private final AlertRepository alertRepository;
    private final RecommendationRepository recommendationRepository;
    private final SimulationRepository simulationRepository;
    private final RiskAnalysisService riskAnalysisService;
    private final CascadeRiskEngine cascadeRiskEngine;

    public DashboardApiController(IncidentRepository incidentRepository,
                                  AlertRepository alertRepository,
                                  RecommendationRepository recommendationRepository,
                                  SimulationRepository simulationRepository,
                                  RiskAnalysisService riskAnalysisService,
                                  CascadeRiskEngine cascadeRiskEngine) {
        this.incidentRepository = incidentRepository;
        this.alertRepository = alertRepository;
        this.recommendationRepository = recommendationRepository;
        this.simulationRepository = simulationRepository;
        this.riskAnalysisService = riskAnalysisService;
        this.cascadeRiskEngine = cascadeRiskEngine;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummary> summary() {
        List<Incident> allIncidents = incidentRepository.findAll();
        List<Incident> openIncidents = allIncidents.stream()
                .filter(i -> i.getStatus() != IncidentStatus.RESOLVED)
                .toList();

        Map<Long, RiskAnalysis> latestRisks = new HashMap<>();
        for (Incident i : allIncidents) {
            RiskAnalysis ra = riskAnalysisService.latestFor(i.getIncidentId());
            if (ra != null) latestRisks.put(i.getIncidentId(), ra);
        }

        long highCriticalOpen = openIncidents.stream()
                .filter(i -> {
                    RiskAnalysis ra = latestRisks.get(i.getIncidentId());
                    return ra != null && (ra.getRiskLevel() == RiskLevel.HIGH || ra.getRiskLevel() == RiskLevel.CRITICAL);
                }).count();

        long activeAlerts = alertRepository.countByAlertStatus(AlertStatus.ACTIVE);
        long criticalAlerts = alertRepository.countByAlertStatusAndPriority(AlertStatus.ACTIVE, Priority.CRITICAL);
        long pendingRecs = recommendationRepository.countByStatus(RecommendationStatus.PENDING);
        long totalIncidents = allIncidents.size();
        long simulationsRun = simulationRepository.count();

        // Distinct zones with >= 1 open high/critical incident
        Set<String> criticalZones = openIncidents.stream()
                .filter(i -> {
                    RiskAnalysis ra = latestRisks.get(i.getIncidentId());
                    return ra != null && (ra.getRiskLevel() == RiskLevel.HIGH || ra.getRiskLevel() == RiskLevel.CRITICAL);
                })
                .map(i -> cascadeRiskEngine.zoneForLocation(i.getLocation()).name())
                .collect(Collectors.toSet());

        // Risk distribution
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (RiskLevel rl : RiskLevel.values()) {
            distribution.put(rl.name(), 0L);
        }
        for (RiskAnalysis ra : latestRisks.values()) {
            if (ra.getRiskLevel() != null) {
                distribution.compute(ra.getRiskLevel().name(), (k, v) -> v == null ? 1L : v + 1L);
            }
        }

        // Zone area risks
        Map<String, List<Incident>> byZone = openIncidents.stream()
                .collect(Collectors.groupingBy(i -> cascadeRiskEngine.zoneForLocation(i.getLocation()).name()));

        List<AreaRisk> areas = new ArrayList<>();
        byZone.forEach((zone, list) -> {
            int maxScore = 0;
            RiskLevel maxLevel = RiskLevel.LOW;
            for (Incident inc : list) {
                RiskAnalysis ra = latestRisks.get(inc.getIncidentId());
                if (ra != null && ra.getRiskScore() > maxScore) {
                    maxScore = (int) Math.round(ra.getRiskScore());
                    maxLevel = ra.getRiskLevel();
                }
            }
            areas.add(new AreaRisk(zone, list.size(), maxScore, maxLevel != null ? maxLevel.name() : "LOW"));
        });
        areas.sort(Comparator.comparing(AreaRisk::maxRiskScore, Comparator.reverseOrder()));

        // Top threat
        TopThreat topThreat = null;
        Incident worstIncident = null;
        int highestScore = -1;
        for (Incident i : openIncidents) {
            RiskAnalysis ra = latestRisks.get(i.getIncidentId());
            if (ra != null && ra.getRiskScore() > highestScore) {
                highestScore = (int) Math.round(ra.getRiskScore());
                worstIncident = i;
            }
        }

        if (worstIncident != null) {
            RiskAnalysis ra = latestRisks.get(worstIncident.getIncidentId());
            SimulationResult sim = cascadeRiskEngine.analyze(riskAnalysisService.inputFor(worstIncident));
            List<CascadeNode> nodes = sim.getNodes().stream().map(n -> new CascadeNode(
                    n.getKey(), n.getLabel(), n.getIcon(), n.getProbability(), n.getSeverity(),
                    n.getLevel().name(), n.getConsequence(), null, null
            )).toList();

            topThreat = new TopThreat(
                    worstIncident.getIncidentId(),
                    worstIncident.getCode(),
                    worstIncident.getLocation(),
                    worstIncident.getIncidentType().name(),
                    (int) Math.round(ra.getRiskScore()),
                    ra.getRiskLevel().name(),
                    sim.getExplanation(),
                    nodes
            );
        }

        return ResponseEntity.ok(new DashboardSummary(
                LocalDateTime.now(),
                "SIMULATED",
                openIncidents.size(),
                highCriticalOpen,
                activeAlerts,
                criticalAlerts,
                criticalZones.size(),
                pendingRecs,
                totalIncidents,
                simulationsRun,
                null,
                distribution,
                areas,
                topThreat
        ));
    }

    @GetMapping("/trend")
    public ResponseEntity<TrendResponse> trend(@RequestParam(defaultValue = "14") int days) {
        int d = Math.min(Math.max(1, days), 90);
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(d - 1);

        List<Incident> all = incidentRepository.findAll();
        Map<LocalDate, Long> countMap = new HashMap<>();
        Map<LocalDate, Long> highRiskMap = new HashMap<>();

        for (Incident i : all) {
            LocalDate dt = i.getReportedDate().toLocalDate();
            if (!dt.isBefore(from) && !dt.isAfter(to)) {
                countMap.merge(dt, 1L, Long::sum);
                RiskAnalysis ra = riskAnalysisService.latestFor(i.getIncidentId());
                if (ra != null && (ra.getRiskLevel() == RiskLevel.HIGH || ra.getRiskLevel() == RiskLevel.CRITICAL)) {
                    highRiskMap.merge(dt, 1L, Long::sum);
                }
            }
        }

        List<TrendPoint> points = new ArrayList<>();
        for (LocalDate cur = from; !cur.isAfter(to); cur = cur.plusDays(1)) {
            points.add(new TrendPoint(
                    cur.toString(),
                    countMap.getOrDefault(cur, 0L),
                    highRiskMap.getOrDefault(cur, 0L)
            ));
        }

        return ResponseEntity.ok(new TrendResponse(from.toString(), to.toString(), points));
    }

    @GetMapping("/activity")
    public ResponseEntity<List<ActivityItem>> activity(@RequestParam(defaultValue = "20") int limit) {
        int l = Math.min(Math.max(1, limit), 100);
        List<ActivityItem> items = new ArrayList<>();

        for (Incident i : incidentRepository.findAll()) {
            items.add(new ActivityItem(
                    "INCIDENT_REPORTED",
                    i.getReportedDate(),
                    i.getIncidentType().getLabel() + " reported",
                    i.getLocation(),
                    i.getIncidentId(),
                    i.getSeverity().name()
            ));
        }

        for (Alert a : alertRepository.findAll()) {
            Long incId = a.getRiskAnalysis() != null && a.getRiskAnalysis().getIncident() != null
                    ? a.getRiskAnalysis().getIncident().getIncidentId() : null;
            items.add(new ActivityItem(
                    "ALERT_RAISED",
                    a.getAlertDate(),
                    "Alert: " + a.getPriority().name(),
                    a.getMessage(),
                    incId,
                    a.getPriority().name()
            ));
        }

        items.sort(Comparator.comparing(ActivityItem::timestamp, Comparator.reverseOrder()));
        return ResponseEntity.ok(items.stream().limit(l).toList());
    }
}
