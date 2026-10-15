package com.civicshield.service;

import com.civicshield.dto.SimulationResult;
import com.civicshield.exception.ResourceNotFoundException;
import com.civicshield.model.Recommendation;
import com.civicshield.model.RecommendationStatus;
import com.civicshield.model.RiskAnalysis;
import com.civicshield.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Stores AI-suggested actions. CivicShield only recommends – approval or dismissal is always a
 * human decision.
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int MAX_PER_ANALYSIS = 4;

    private final RecommendationRepository repository;

    public List<Recommendation> list(RecommendationStatus status) {
        return status == null ? repository.findAllByOrderByRecommendationIdDesc()
                : repository.findByStatusOrderByRecommendationIdDesc(status);
    }

    public List<Recommendation> forIncident(Long incidentId) {
        return repository.findByRiskAnalysis_Incident_IncidentIdOrderByRecommendationIdAsc(incidentId);
    }

    public Map<RecommendationStatus, Long> counts() {
        Map<RecommendationStatus, Long> m = new EnumMap<>(RecommendationStatus.class);
        for (RecommendationStatus s : RecommendationStatus.values()) m.put(s, repository.countByStatus(s));
        return m;
    }

    @Transactional
    public void createForAnalysis(RiskAnalysis ra, SimulationResult r) {
        r.getActions().stream().limit(MAX_PER_ANALYSIS).forEach(a -> repository.save(Recommendation.builder()
                .riskAnalysis(ra)
                .action(a.getText())
                .priority(a.getPriority())
                .status(RecommendationStatus.PENDING)
                .build()));
    }

    @Transactional
    public Recommendation approve(Long id) {
        Recommendation r = get(id);
        r.setStatus(RecommendationStatus.APPROVED);
        return r;
    }

    @Transactional
    public Recommendation dismiss(Long id) {
        Recommendation r = get(id);
        r.setStatus(RecommendationStatus.DISMISSED);
        return r;
    }

    private Recommendation get(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Recommendation", id));
    }
}
