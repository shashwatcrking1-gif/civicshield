package com.civicshield.api.v1.controller;

import com.civicshield.api.v1.auth.TokenService;
import com.civicshield.api.v1.dto.AlertRecommendationDtos.AlertDto;
import com.civicshield.api.v1.dto.AlertRecommendationDtos.RecommendationDto;
import com.civicshield.api.v1.dto.IncidentDtos.*;
import com.civicshield.api.v1.dto.PageResponse;
import com.civicshield.dto.IncidentForm;
import com.civicshield.exception.ApiException;
import com.civicshield.model.*;
import com.civicshield.repository.UserRepository;
import com.civicshield.service.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentApiController {

    private final IncidentService incidentService;
    private final RiskAnalysisService riskAnalysisService;
    private final CascadeRiskEngine cascadeRiskEngine;
    private final AlertService alertService;
    private final RecommendationService recommendationService;
    private final UserRepository userRepository;

    public IncidentApiController(IncidentService incidentService,
                                 RiskAnalysisService riskAnalysisService,
                                 CascadeRiskEngine cascadeRiskEngine,
                                 AlertService alertService,
                                 RecommendationService recommendationService,
                                 UserRepository userRepository) {
        this.incidentService = incidentService;
        this.riskAnalysisService = riskAnalysisService;
        this.cascadeRiskEngine = cascadeRiskEngine;
        this.alertService = alertService;
        this.recommendationService = recommendationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<PageResponse<IncidentSummary>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) IncidentType type,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) String zone,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "reportedAt,desc") String sort) {

        int validSize = Math.min(Math.max(1, size), 100);
        org.springframework.data.domain.Page<Incident> results = incidentService.search(q, type, severity, status, page, validSize);

        List<IncidentSummary> dtos = results.getContent().stream()
                .filter(i -> zone == null || zone.isBlank() || cascadeRiskEngine.zoneForLocation(i.getLocation()).name().equalsIgnoreCase(zone.trim()))
                .map(this::toSummary)
                .toList();

        return ResponseEntity.ok(new PageResponse<>(dtos, results.getNumber(), validSize, results.getTotalElements(), results.getTotalPages()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentDetail> get(@PathVariable Long id) {
        Incident incident = incidentService.get(id);
        return ResponseEntity.ok(toDetail(incident));
    }

    @PostMapping
    public ResponseEntity<IncidentDetail> create(@Valid @RequestBody IncidentRequest req, Authentication authentication) {
        User user = resolveUser(authentication);
        IncidentType type = parseType(req.type());
        Severity severity = parseSeverity(req.severity());

        IncidentForm form = new IncidentForm();
        form.setIncidentType(type);
        form.setLocation(req.location());
        form.setLatitude(req.latitude());
        form.setLongitude(req.longitude());
        form.setSeverity(severity);

        Incident created = incidentService.create(form, user);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.getIncidentId()).toUri();
        return ResponseEntity.created(location).body(toDetail(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<IncidentDetail> update(@PathVariable Long id, @Valid @RequestBody IncidentRequest req) {
        Incident incident = incidentService.get(id);
        if (incident.getStatus() == IncidentStatus.RESOLVED) {
            throw ApiException.conflict("Cannot edit a resolved incident");
        }

        incident.setIncidentType(parseType(req.type()));
        incident.setLocation(req.location().trim());
        incident.setLatitude(req.latitude());
        incident.setLongitude(req.longitude());
        incident.setSeverity(parseSeverity(req.severity()));

        riskAnalysisService.analyzeAndStore(incident, LocalDateTime.now());
        return ResponseEntity.ok(toDetail(incident));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<IncidentDetail> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest req) {
        IncidentStatus status;
        try {
            status = IncidentStatus.valueOf(req.status().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Invalid status: " + req.status());
        }

        Incident updated = incidentService.updateStatus(id, status);
        return ResponseEntity.ok(toDetail(updated));
    }

    @GetMapping("/{id}/alerts")
    public ResponseEntity<List<AlertDto>> alerts(@PathVariable Long id) {
        Incident incident = incidentService.get(id);
        String zone = cascadeRiskEngine.zoneForLocation(incident.getLocation()).name();
        List<AlertDto> dtos = alertService.forIncident(id).stream()
                .map(a -> AlertDto.from(a, zone))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}/recommendations")
    public ResponseEntity<List<RecommendationDto>> recommendations(@PathVariable Long id) {
        Incident incident = incidentService.get(id);
        String zone = cascadeRiskEngine.zoneForLocation(incident.getLocation()).name();
        List<RecommendationDto> dtos = recommendationService.forIncident(id).stream()
                .map(r -> RecommendationDto.from(r, zone))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    private IncidentSummary toSummary(Incident i) {
        RiskAnalysis ra = riskAnalysisService.latestFor(i.getIncidentId());
        String zone = cascadeRiskEngine.zoneForLocation(i.getLocation()).name();
        return IncidentSummary.from(i, RiskSnapshot.from(ra), zone);
    }

    private IncidentDetail toDetail(Incident i) {
        RiskAnalysis ra = riskAnalysisService.latestFor(i.getIncidentId());
        List<RiskAnalysis> analyses = riskAnalysisService.forIncident(i.getIncidentId());
        String zone = cascadeRiskEngine.zoneForLocation(i.getLocation()).name();
        return new IncidentDetail(
                i.getIncidentId(),
                i.getCode(),
                i.getIncidentType().name(),
                i.getLocation(),
                zone,
                i.getLatitude(),
                i.getLongitude(),
                i.getSeverity().name(),
                i.getStatus().name(),
                i.getReportedDate(),
                i.getReportedBy() != null ? i.getReportedBy().getName() : "Anonymous",
                RiskSnapshot.from(ra),
                ra != null ? ra.getPredictedImpact() : "No impact predicted yet.",
                analyses.size()
        );
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null) return null;
        String email = authentication.getName();
        if (authentication.getPrincipal() instanceof TokenService.TokenInfo info) {
            email = info.email();
        }
        return userRepository.findByEmailIgnoreCase(email).orElse(null);
    }

    private IncidentType parseType(String type) {
        try {
            return IncidentType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw ApiException.badRequest("Invalid incident type: " + type);
        }
    }

    private Severity parseSeverity(String severity) {
        try {
            return Severity.valueOf(severity.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw ApiException.badRequest("Invalid severity: " + severity);
        }
    }
}
