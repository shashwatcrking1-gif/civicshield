package com.civicshield.service;

import com.civicshield.dto.DashboardStats;
import com.civicshield.dto.SimulationResult;
import com.civicshield.model.*;
import com.civicshield.repository.IncidentRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/** Dashboard aggregates, trends, filtered reports and CSV export. */
@Service
public class ReportService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final IncidentRepository incidentRepository;
    private final RiskAnalysisService riskAnalysisService;
    private final AlertService alertService;
    private final SimulationService simulationService;
    private final CascadeRiskEngine engine;

    public ReportService(IncidentRepository incidentRepository, RiskAnalysisService riskAnalysisService,
                         AlertService alertService, SimulationService simulationService,
                         CascadeRiskEngine engine) {
        this.incidentRepository = incidentRepository;
        this.riskAnalysisService = riskAnalysisService;
        this.alertService = alertService;
        this.simulationService = simulationService;
        this.engine = engine;
    }

    /** One row of the reports table. */
    @Getter
    public static class ReportRow {
        private final Incident incident;
        private final RiskAnalysis analysis;

        public ReportRow(Incident incident, RiskAnalysis analysis) {
            this.incident = incident;
            this.analysis = analysis;
        }
    }

    public DashboardStats dashboardStats() {
        List<Incident> all = incidentRepository.findAllByOrderByReportedDateDesc();
        Map<Long, RiskAnalysis> latest = riskAnalysisService.latestByIncident();
        List<Incident> open = all.stream().filter(i -> i.getStatus() != IncidentStatus.RESOLVED).toList();

        long highRiskZones = open.stream()
                .filter(i -> isHigh(latest.get(i.getIncidentId())))
                .map(i -> engine.zoneForLocation(i.getLocation()).name())
                .distinct().count();

        Map<String, Long> dist = new LinkedHashMap<>();
        for (RiskLevel l : RiskLevel.values()) dist.put(l.getLabel(), 0L);
        all.forEach(i -> {
            RiskAnalysis ra = latest.get(i.getIncidentId());
            if (ra != null) dist.merge(ra.getRiskLevel().getLabel(), 1L, Long::sum);
        });

        DashboardStats.DashboardStatsBuilder b = DashboardStats.builder()
                .activeIncidents(open.size())
                .highRiskZones(highRiskZones)
                .criticalAlerts(alertService.criticalActiveCount())
                .simulationsRun(simulationService.count())
                .totalIncidents(all.size())
                .riskDistribution(dist);

        open.stream()
                .filter(i -> latest.containsKey(i.getIncidentId()))
                .max(Comparator.comparingDouble(i -> latest.get(i.getIncidentId()).getRiskScore()))
                .ifPresent(top -> {
                    SimulationResult r = riskAnalysisService.evaluate(top);
                    b.topThreatIncidentId(top.getIncidentId())
                            .topThreatCode(top.getCode())
                            .topThreatLocation(top.getLocation())
                            .topThreatType(top.getIncidentType().getLabel())
                            .topThreatScore(r.getRiskScore())
                            .topThreatLevel(r.getRiskLevel())
                            .topThreatExplanation(r.getExplanation())
                            .topThreatNodes(r.getNodes());
                });
        return b.build();
    }

    /** Incidents reported per day for the last {@code days} days, plus how many were High/Critical. */
    public Map<String, Object> trend(int days) {
        LocalDate start = LocalDate.now().minusDays(days - 1L);
        Map<Long, RiskAnalysis> latest = riskAnalysisService.latestByIncident();
        List<Incident> recent = incidentRepository.findByReportedDateAfterOrderByReportedDateAsc(start.atStartOfDay());
        List<String> labels = new ArrayList<>();
        List<Long> total = new ArrayList<>(), high = new ArrayList<>();
        for (int d = 0; d < days; d++) {
            LocalDate day = start.plusDays(d);
            labels.add(day.format(DAY));
            List<Incident> onDay = recent.stream().filter(i -> i.getReportedDate().toLocalDate().equals(day)).toList();
            total.add((long) onDay.size());
            high.add(onDay.stream().filter(i -> isHigh(latest.get(i.getIncidentId()))).count());
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("labels", labels);
        m.put("incidents", total);
        m.put("highRisk", high);
        return m;
    }

    public List<ReportRow> rows(LocalDate from, LocalDate to, IncidentType type, RiskLevel level, String q) {
        Map<Long, RiskAnalysis> latest = riskAnalysisService.latestByIncident();
        Long id = q == null || q.isBlank() ? null : IncidentService.parseCode(q.trim().toLowerCase(Locale.ROOT));
        return incidentRepository.findAllByOrderByReportedDateDesc().stream()
                .filter(i -> from == null || !i.getReportedDate().toLocalDate().isBefore(from))
                .filter(i -> to == null || !i.getReportedDate().toLocalDate().isAfter(to))
                .filter(i -> type == null || i.getIncidentType() == type)
                .filter(i -> level == null || (latest.get(i.getIncidentId()) != null
                        && latest.get(i.getIncidentId()).getRiskLevel() == level))
                .filter(i -> q == null || q.isBlank() || Objects.equals(i.getIncidentId(), id)
                        || i.getLocation().toLowerCase(Locale.ROOT).contains(q.trim().toLowerCase(Locale.ROOT)))
                .map(i -> new ReportRow(i, latest.get(i.getIncidentId())))
                .toList();
    }

    public Map<String, Long> countByType(List<ReportRow> rows) {
        return rows.stream().collect(Collectors.groupingBy(r -> r.getIncident().getIncidentType().getLabel(),
                LinkedHashMap::new, Collectors.counting()));
    }

    public Map<String, Long> countByLevel(List<ReportRow> rows) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (RiskLevel l : RiskLevel.values()) m.put(l.getLabel(), 0L);
        rows.stream().filter(r -> r.getAnalysis() != null)
                .forEach(r -> m.merge(r.getAnalysis().getRiskLevel().getLabel(), 1L, Long::sum));
        return m;
    }

    public Map<String, Long> countByStatus(List<ReportRow> rows) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (IncidentStatus s : IncidentStatus.values()) m.put(s.getLabel(), 0L);
        rows.forEach(r -> m.merge(r.getIncident().getStatus().getLabel(), 1L, Long::sum));
        return m;
    }

    /** RFC-4180 style CSV. */
    public String toCsv(List<ReportRow> rows) {
        StringBuilder sb = new StringBuilder("Incident ID,Type,Location,Latitude,Longitude,Severity,Status,Reported,Risk Score,Risk Level,Predicted Impact,Data\n");
        for (ReportRow r : rows) {
            Incident i = r.getIncident();
            RiskAnalysis a = r.getAnalysis();
            sb.append(String.join(",",
                    i.getCode(), csv(i.getIncidentType().getLabel()), csv(i.getLocation()),
                    String.valueOf(i.getLatitude()), String.valueOf(i.getLongitude()),
                    i.getSeverity().getLabel(), i.getStatus().getLabel(), i.getReportedDate().format(TS),
                    a == null ? "" : String.valueOf(Math.round(a.getRiskScore())),
                    a == null ? "" : a.getRiskLevel().getLabel(),
                    a == null ? "" : csv(a.getPredictedImpact()),
                    "Simulated data")).append('\n');
        }
        return sb.toString();
    }

    private static String csv(String v) {
        if (v == null) return "";
        String s = v.replace("\"", "\"\"");
        // Neutralise spreadsheet formula injection
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        return "\"" + s + "\"";
    }

    private static boolean isHigh(RiskAnalysis ra) {
        return ra != null && (ra.getRiskLevel() == RiskLevel.HIGH || ra.getRiskLevel() == RiskLevel.CRITICAL);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }
}
