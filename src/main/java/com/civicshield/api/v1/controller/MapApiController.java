package com.civicshield.api.v1.controller;

import com.civicshield.api.v1.dto.CascadeDtos.ZoneDto;
import com.civicshield.api.v1.dto.IncidentDtos.MapIncident;
import com.civicshield.model.Incident;
import com.civicshield.model.IncidentStatus;
import com.civicshield.model.RiskAnalysis;
import com.civicshield.service.CascadeRiskEngine;
import com.civicshield.service.CascadeRiskEngine.ZoneProfile;
import com.civicshield.service.IncidentService;
import com.civicshield.service.RiskAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1")
public class MapApiController {

    private final IncidentService incidentService;
    private final RiskAnalysisService riskAnalysisService;
    private final CascadeRiskEngine cascadeRiskEngine;

    public MapApiController(IncidentService incidentService,
                            RiskAnalysisService riskAnalysisService,
                            CascadeRiskEngine cascadeRiskEngine) {
        this.incidentService = incidentService;
        this.riskAnalysisService = riskAnalysisService;
        this.cascadeRiskEngine = cascadeRiskEngine;
    }

    @GetMapping("/map/incidents")
    public ResponseEntity<List<MapIncident>> mapIncidents(@RequestParam(defaultValue = "false") boolean includeResolved) {
        List<Incident> list = includeResolved ? incidentService.all() : incidentService.open();
        List<MapIncident> dtos = list.stream().map(i -> {
            RiskAnalysis ra = riskAnalysisService.latestFor(i.getIncidentId());
            String zone = cascadeRiskEngine.zoneForLocation(i.getLocation()).name();
            Integer score = ra != null ? (int) Math.round(ra.getRiskScore()) : null;
            String level = ra != null && ra.getRiskLevel() != null ? ra.getRiskLevel().name() : null;
            String summary = ra != null && ra.getPredictedImpact() != null ? ra.getPredictedImpact() : "";
            return new MapIncident(
                    i.getIncidentId(),
                    i.getCode(),
                    i.getIncidentType().name(),
                    i.getLocation(),
                    zone,
                    i.getLatitude(),
                    i.getLongitude(),
                    i.getSeverity().name(),
                    i.getStatus().name(),
                    score,
                    level,
                    summary,
                    i.getReportedDate()
            );
        }).toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/zones")
    public ResponseEntity<List<ZoneDto>> listZones() {
        List<ZoneDto> dtos = cascadeRiskEngine.zones().stream().map(z -> new ZoneDto(
                z.name().toLowerCase(Locale.ROOT).replace(" ", "-"),
                z.name(),
                z.lat(),
                z.lng(),
                z.drainage(),
                z.roadVulnerability(),
                z.historical(),
                z.hospitalLoad(),
                z.exposure(),
                z.baseTraffic(),
                z.diversionRoute(),
                z.hospital(),
                true
        )).toList();

        return ResponseEntity.ok(dtos);
    }
}
