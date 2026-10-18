package com.civicshield.service;

import com.civicshield.dto.SimulationResult;
import com.civicshield.model.Incident;
import com.civicshield.model.IncidentType;
import com.civicshield.model.RiskAnalysis;
import com.civicshield.model.Severity;
import com.civicshield.repository.RiskAnalysisRepository;
import com.civicshield.service.CascadeRiskEngine.EngineInput;
import com.civicshield.service.CascadeRiskEngine.ZoneProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Bridges incidents and the {@link CascadeRiskEngine}: converts an incident into engine inputs,
 * stores the analysis and fans out alerts and recommendations.
 */
@Service
public class RiskAnalysisService {

    private final CascadeRiskEngine engine;
    private final RiskAnalysisRepository repository;
    private final AlertService alertService;
    private final RecommendationService recommendationService;

    public RiskAnalysisService(CascadeRiskEngine engine, RiskAnalysisRepository repository,
                               AlertService alertService, RecommendationService recommendationService) {
        this.engine = engine;
        this.repository = repository;
        this.alertService = alertService;
        this.recommendationService = recommendationService;
    }

    /**
     * Maps an incident to engine inputs. Weather incidents set the rainfall from severity; other types
     * strengthen the local factor of the node they affect directly.
     */
    public EngineInput inputFor(Incident i) {
        ZoneProfile z = engine.zoneForLocation(i.getLocation());
        IncidentType t = i.getIncidentType();
        int boost = severityBoost(i.getSeverity());

        double rainfall = t.isWeatherDriven() ? switch (i.getSeverity()) {
            case LOW -> 30;
            case MEDIUM -> 60;
            case HIGH -> 90;
            case CRITICAL -> 115;
        } : 15; // ambient monsoon drizzle for non-weather incidents

        double drainage = z.drainage() - (t == IncidentType.DRAINAGE_OVERFLOW ? boost : 0);
        double traffic = Math.min(100, z.baseTraffic() + (t == IncidentType.TRAFFIC_CONGESTION ? boost / 2.0 : 0));
        double road = Math.min(100, z.roadVulnerability() + switch (t) {
            case ROAD_BLOCKAGE, TREE_FALL, ROAD_ACCIDENT -> boost;
            default -> 0;
        });
        double hospital = Math.min(100, z.hospitalLoad() + switch (t) {
            case POWER_OUTAGE, MEDICAL_EMERGENCY, FIRE -> boost;
            default -> 0;
        });
        return new EngineInput(z.name(), rainfall, drainage, traffic, road, z.historical(), hospital,
                z.exposure(), false, false, false, false);
    }

    /** Re-computes the full cascade view for an incident (deterministic). */
    public SimulationResult evaluate(Incident incident) {
        return engine.analyze(inputFor(incident));
    }

    @Transactional
    public RiskAnalysis analyzeAndStore(Incident incident, LocalDateTime at) {
        SimulationResult r = evaluate(incident);
        RiskAnalysis ra = repository.save(RiskAnalysis.builder()
                .incident(incident)
                .riskScore((double) r.getRiskScore())
                .riskLevel(r.getRiskLevel())
                .predictedImpact(buildImpact(r))
                .analysisDate(at)
                .build());
        alertService.createForAnalysis(ra, r, at);
        recommendationService.createForAnalysis(ra, r);
        return ra;
    }

    public Optional<RiskAnalysis> latest(Incident incident) {
        return repository.findTopByIncidentOrderByAnalysisDateDesc(incident);
    }

    /** Latest analysis per incident id (lists, map, reports). */
    public Map<Long, RiskAnalysis> latestByIncident() {
        Map<Long, RiskAnalysis> map = new HashMap<>();
        for (RiskAnalysis ra : repository.findAllByOrderByAnalysisDateDesc()) {
            map.putIfAbsent(ra.getIncident().getIncidentId(), ra);
        }
        return map;
    }

    public RiskAnalysis latestFor(Long incidentId) {
        return latestByIncident().get(incidentId);
    }

    public java.util.List<RiskAnalysis> forIncident(Long incidentId) {
        return repository.findByIncident_IncidentIdOrderByAnalysisDateDesc(incidentId);
    }

    private String buildImpact(SimulationResult r) {
        String text = r.getExplanation() + " Predicted chain: " + r.chainSummary(6) + ".";
        return text.length() > 2000 ? text.substring(0, 2000) : text;
    }

    private static int severityBoost(Severity s) {
        return switch (s) {
            case LOW -> 5;
            case MEDIUM -> 12;
            case HIGH -> 20;
            case CRITICAL -> 28;
        };
    }
}
