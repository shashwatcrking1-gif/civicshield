package com.civicshield.api.v1.controller;

import com.civicshield.api.v1.dto.AlertRecommendationDtos.AlertDto;
import com.civicshield.api.v1.dto.PageResponse;
import com.civicshield.exception.ApiException;
import com.civicshield.model.Alert;
import com.civicshield.model.AlertStatus;
import com.civicshield.model.Priority;
import com.civicshield.repository.AlertRepository;
import com.civicshield.service.AlertService;
import com.civicshield.service.CascadeRiskEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertApiController {

    private final AlertService alertService;
    private final AlertRepository alertRepository;
    private final CascadeRiskEngine cascadeRiskEngine;

    public AlertApiController(AlertService alertService,
                              AlertRepository alertRepository,
                              CascadeRiskEngine cascadeRiskEngine) {
        this.alertService = alertService;
        this.alertRepository = alertRepository;
        this.cascadeRiskEngine = cascadeRiskEngine;
    }

    @GetMapping
    public ResponseEntity<PageResponse<AlertDto>> list(
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int validSize = Math.min(Math.max(1, size), 100);
        List<Alert> all = alertRepository.findAll().stream()
                .sorted(Comparator.comparing(Alert::getAlertDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .filter(a -> status == null || a.getAlertStatus() == status)
                .filter(a -> priority == null || a.getPriority() == priority)
                .filter(a -> {
                    if (q == null || q.isBlank()) return true;
                    String term = q.trim().toLowerCase(Locale.ROOT);
                    boolean matchMsg = a.getMessage() != null && a.getMessage().toLowerCase(Locale.ROOT).contains(term);
                    boolean matchLoc = a.getRiskAnalysis() != null && a.getRiskAnalysis().getIncident() != null
                            && a.getRiskAnalysis().getIncident().getLocation().toLowerCase(Locale.ROOT).contains(term);
                    boolean matchCode = a.getRiskAnalysis() != null && a.getRiskAnalysis().getIncident() != null
                            && a.getRiskAnalysis().getIncident().getCode().toLowerCase(Locale.ROOT).contains(term);
                    return matchMsg || matchLoc || matchCode;
                })
                .toList();

        List<AlertDto> dtos = all.stream().map(a -> {
            String zone = a.getRiskAnalysis() != null && a.getRiskAnalysis().getIncident() != null
                    ? cascadeRiskEngine.zoneForLocation(a.getRiskAnalysis().getIncident().getLocation()).name()
                    : "Chennai";
            return AlertDto.from(a, zone);
        }).toList();

        return ResponseEntity.ok(PageResponse.of(dtos, page, validSize));
    }

    @GetMapping("/counts")
    public ResponseEntity<Map<String, Long>> counts() {
        Map<AlertStatus, Long> counts = alertService.counts();
        Map<String, Long> result = new LinkedHashMap<>();
        for (AlertStatus s : AlertStatus.values()) {
            result.put(s.name(), counts.getOrDefault(s, 0L));
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<AlertDto> acknowledge(@PathVariable Long id) {
        Alert alert = alertRepository.findById(id).orElseThrow(() -> ApiException.notFound("Alert " + id + " not found"));
        if (alert.getAlertStatus() != AlertStatus.ACTIVE) {
            throw ApiException.conflict("Alert is not in ACTIVE status");
        }
        Alert updated = alertService.acknowledge(id);
        String zone = updated.getRiskAnalysis() != null && updated.getRiskAnalysis().getIncident() != null
                ? cascadeRiskEngine.zoneForLocation(updated.getRiskAnalysis().getIncident().getLocation()).name()
                : "Chennai";
        return ResponseEntity.ok(AlertDto.from(updated, zone));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<AlertDto> resolve(@PathVariable Long id) {
        Alert alert = alertRepository.findById(id).orElseThrow(() -> ApiException.notFound("Alert " + id + " not found"));
        if (alert.getAlertStatus() == AlertStatus.RESOLVED) {
            throw ApiException.conflict("Alert is already RESOLVED");
        }
        Alert updated = alertService.resolve(id);
        String zone = updated.getRiskAnalysis() != null && updated.getRiskAnalysis().getIncident() != null
                ? cascadeRiskEngine.zoneForLocation(updated.getRiskAnalysis().getIncident().getLocation()).name()
                : "Chennai";
        return ResponseEntity.ok(AlertDto.from(updated, zone));
    }
}
