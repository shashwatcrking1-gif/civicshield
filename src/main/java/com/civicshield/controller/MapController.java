package com.civicshield.controller;

import com.civicshield.dto.SimulationResult;
import com.civicshield.model.Incident;
import com.civicshield.model.RiskAnalysis;
import com.civicshield.service.CascadeRiskEngine;
import com.civicshield.service.IncidentService;
import com.civicshield.service.RiskAnalysisService;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Interactive Leaflet risk map view and GeoJSON/JSON incident markers endpoint.
 */
@Controller
public class MapController {

    private final IncidentService incidentService;
    private final RiskAnalysisService riskAnalysisService;
    private final CascadeRiskEngine engine;

    public MapController(IncidentService incidentService, RiskAnalysisService riskAnalysisService, CascadeRiskEngine engine) {
        this.incidentService = incidentService;
        this.riskAnalysisService = riskAnalysisService;
        this.engine = engine;
    }

    @GetMapping("/portal/map")
    public String mapPage(Model model) {
        model.addAttribute("activeNav", "map");
        model.addAttribute("zones", engine.zones());
        return "portal/map";
    }

    @GetMapping("/api/map/incidents")
    @ResponseBody
    public ResponseEntity<List<MapIncidentItem>> getMapIncidents() {
        List<Incident> incidents = incidentService.all();
        Map<Long, RiskAnalysis> latestAnalyses = riskAnalysisService.latestByIncident();

        List<MapIncidentItem> items = new ArrayList<>();
        for (Incident i : incidents) {
            RiskAnalysis ra = latestAnalyses.get(i.getIncidentId());
            SimulationResult cascade = riskAnalysisService.evaluate(i);

            items.add(MapIncidentItem.builder()
                    .incidentId(i.getIncidentId())
                    .code(i.getCode())
                    .type(i.getIncidentType().getLabel())
                    .typeIcon(i.getIncidentType().getIcon())
                    .location(i.getLocation())
                    .latitude(i.getLatitude())
                    .longitude(i.getLongitude())
                    .severity(i.getSeverity().getLabel())
                    .severityCss(i.getSeverity().getCss())
                    .status(i.getStatus().getLabel())
                    .statusCss(i.getStatus().getCss())
                    .riskScore(ra != null ? (int) Math.round(ra.getRiskScore()) : cascade.getRiskScore())
                    .riskLevel(ra != null ? ra.getRiskLevel().getLabel() : cascade.getRiskLevel().getLabel())
                    .riskColor(ra != null ? ra.getRiskLevel().getColor() : cascade.getRiskLevel().getColor())
                    .chainSummary(cascade.chainSummary(4))
                    .explanation(cascade.getExplanation())
                    .reportedDate(i.getReportedDate().toString())
                    .build());
        }
        return ResponseEntity.ok(items);
    }

    @Data
    @Builder
    public static class MapIncidentItem {
        private Long incidentId;
        private String code;
        private String type;
        private String typeIcon;
        private String location;
        private Double latitude;
        private Double longitude;
        private String severity;
        private String severityCss;
        private String status;
        private String statusCss;
        private Integer riskScore;
        private String riskLevel;
        private String riskColor;
        private String chainSummary;
        private String explanation;
        private String reportedDate;

        public MapIncidentItem() {
        }

        public MapIncidentItem(Long incidentId, String code, String type, String typeIcon,
                               String location, Double latitude, Double longitude, String severity,
                               String severityCss, String status, String statusCss, Integer riskScore,
                               String riskLevel, String riskColor, String chainSummary, String explanation,
                               String reportedDate) {
            this.incidentId = incidentId;
            this.code = code;
            this.type = type;
            this.typeIcon = typeIcon;
            this.location = location;
            this.latitude = latitude;
            this.longitude = longitude;
            this.severity = severity;
            this.severityCss = severityCss;
            this.status = status;
            this.statusCss = statusCss;
            this.riskScore = riskScore;
            this.riskLevel = riskLevel;
            this.riskColor = riskColor;
            this.chainSummary = chainSummary;
            this.explanation = explanation;
            this.reportedDate = reportedDate;
        }
    }
}
