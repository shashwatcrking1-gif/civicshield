package com.civicshield.api.v1.controller;

import com.civicshield.api.v1.dto.AlertRecommendationDtos.RecommendationDto;
import com.civicshield.api.v1.dto.PageResponse;
import com.civicshield.exception.ApiException;
import com.civicshield.model.Priority;
import com.civicshield.model.Recommendation;
import com.civicshield.model.RecommendationStatus;
import com.civicshield.repository.RecommendationRepository;
import com.civicshield.service.CascadeRiskEngine;
import com.civicshield.service.RecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationApiController {

    private final RecommendationService recommendationService;
    private final RecommendationRepository recommendationRepository;
    private final CascadeRiskEngine cascadeRiskEngine;

    public RecommendationApiController(RecommendationService recommendationService,
                                        RecommendationRepository recommendationRepository,
                                        CascadeRiskEngine cascadeRiskEngine) {
        this.recommendationService = recommendationService;
        this.recommendationRepository = recommendationRepository;
        this.cascadeRiskEngine = cascadeRiskEngine;
    }

    @GetMapping
    public ResponseEntity<PageResponse<RecommendationDto>> list(
            @RequestParam(required = false) RecommendationStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int validSize = Math.min(Math.max(1, size), 100);
        List<Recommendation> all = recommendationRepository.findAll().stream()
                .sorted(Comparator.comparing(Recommendation::getRecommendationId, Comparator.reverseOrder()))
                .filter(r -> status == null || r.getStatus() == status)
                .filter(r -> priority == null || r.getPriority() == priority)
                .filter(r -> {
                    if (q == null || q.isBlank()) return true;
                    String term = q.trim().toLowerCase(Locale.ROOT);
                    boolean matchAction = r.getAction() != null && r.getAction().toLowerCase(Locale.ROOT).contains(term);
                    boolean matchLoc = r.getRiskAnalysis() != null && r.getRiskAnalysis().getIncident() != null
                            && r.getRiskAnalysis().getIncident().getLocation().toLowerCase(Locale.ROOT).contains(term);
                    boolean matchCode = r.getRiskAnalysis() != null && r.getRiskAnalysis().getIncident() != null
                            && r.getRiskAnalysis().getIncident().getCode().toLowerCase(Locale.ROOT).contains(term);
                    return matchAction || matchLoc || matchCode;
                })
                .toList();

        List<RecommendationDto> dtos = all.stream().map(r -> {
            String zone = r.getRiskAnalysis() != null && r.getRiskAnalysis().getIncident() != null
                    ? cascadeRiskEngine.zoneForLocation(r.getRiskAnalysis().getIncident().getLocation()).name()
                    : "Chennai";
            return RecommendationDto.from(r, zone);
        }).toList();

        return ResponseEntity.ok(PageResponse.of(dtos, page, validSize));
    }

    @GetMapping("/counts")
    public ResponseEntity<Map<String, Long>> counts() {
        Map<RecommendationStatus, Long> counts = recommendationService.counts();
        Map<String, Long> result = new LinkedHashMap<>();
        for (RecommendationStatus s : RecommendationStatus.values()) {
            result.put(s.name(), counts.getOrDefault(s, 0L));
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<RecommendationDto> approve(@PathVariable Long id) {
        Recommendation rec = recommendationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Recommendation " + id + " not found"));
        if (rec.getStatus() != RecommendationStatus.PENDING) {
            throw ApiException.conflict("Recommendation is not PENDING");
        }
        Recommendation updated = recommendationService.approve(id);
        String zone = updated.getRiskAnalysis() != null && updated.getRiskAnalysis().getIncident() != null
                ? cascadeRiskEngine.zoneForLocation(updated.getRiskAnalysis().getIncident().getLocation()).name()
                : "Chennai";
        return ResponseEntity.ok(RecommendationDto.from(updated, zone));
    }

    @PostMapping("/{id}/dismiss")
    public ResponseEntity<RecommendationDto> dismiss(@PathVariable Long id) {
        Recommendation rec = recommendationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Recommendation " + id + " not found"));
        if (rec.getStatus() != RecommendationStatus.PENDING) {
            throw ApiException.conflict("Recommendation is not PENDING");
        }
        Recommendation updated = recommendationService.dismiss(id);
        String zone = updated.getRiskAnalysis() != null && updated.getRiskAnalysis().getIncident() != null
                ? cascadeRiskEngine.zoneForLocation(updated.getRiskAnalysis().getIncident().getLocation()).name()
                : "Chennai";
        return ResponseEntity.ok(RecommendationDto.from(updated, zone));
    }
}
