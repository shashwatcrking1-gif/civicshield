package com.civicshield.api.v1.controller;

import com.civicshield.api.v1.dto.CascadeDtos.*;
import com.civicshield.dto.CascadeNodeDto;
import com.civicshield.dto.FactorDto;
import com.civicshield.dto.SimulationResult;
import com.civicshield.dto.SimulationResult.ActionDto;
import com.civicshield.model.Incident;
import com.civicshield.model.RiskAnalysis;
import com.civicshield.service.CascadeRiskEngine;
import com.civicshield.service.CascadeRiskEngine.EngineInput;
import com.civicshield.service.IncidentService;
import com.civicshield.service.RiskAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class RiskApiController {

    private final IncidentService incidentService;
    private final RiskAnalysisService riskAnalysisService;
    private final CascadeRiskEngine cascadeRiskEngine;

    public RiskApiController(IncidentService incidentService,
                             RiskAnalysisService riskAnalysisService,
                             CascadeRiskEngine cascadeRiskEngine) {
        this.incidentService = incidentService;
        this.riskAnalysisService = riskAnalysisService;
        this.cascadeRiskEngine = cascadeRiskEngine;
    }

    @GetMapping("/incidents/{id}/cascade")
    public ResponseEntity<IncidentCascadeResponse> getCascade(@PathVariable Long id) {
        Incident incident = incidentService.get(id);
        RiskAnalysis ra = riskAnalysisService.latestFor(id);
        return ResponseEntity.ok(buildCascadeResponse(incident, ra));
    }

    @PostMapping("/incidents/{id}/cascade")
    public ResponseEntity<IncidentCascadeResponse> reanalyzeCascade(@PathVariable Long id) {
        Incident incident = incidentService.get(id);
        RiskAnalysis ra = riskAnalysisService.analyzeAndStore(incident, LocalDateTime.now());
        return ResponseEntity.ok(buildCascadeResponse(incident, ra));
    }

    @GetMapping("/risk/model")
    public ResponseEntity<RiskModelResponse> getRiskModel() {
        Method method = Method.standard();
        List<Map<String, Object>> chain = List.of(
                Map.of("key", "rain", "label", "Rain", "severity", 0.40),
                Map.of("key", "flood", "label", "Flood", "severity", 0.70),
                Map.of("key", "road", "label", "Road Blockage", "severity", 0.60),
                Map.of("key", "traffic", "label", "Traffic", "severity", 0.50),
                Map.of("key", "ambulance", "label", "Ambulance Delay", "severity", 0.90),
                Map.of("key", "hospital", "label", "Hospital Impact", "severity", 1.00)
        );

        List<CascadeEdge> baseWeights = List.of(
                new CascadeEdge("rain", "flood", 0.70, 0.70, "Rain → Flood dependency"),
                new CascadeEdge("flood", "road", 0.75, 0.75, "Flood → Road Blockage dependency"),
                new CascadeEdge("road", "traffic", 0.60, 0.60, "Road Blockage → Traffic dependency"),
                new CascadeEdge("traffic", "ambulance", 0.65, 0.65, "Traffic → Ambulance Delay dependency"),
                new CascadeEdge("ambulance", "hospital", 0.55, 0.55, "Ambulance Delay → Hospital Impact dependency")
        );

        return ResponseEntity.ok(new RiskModelResponse(method, chain, baseWeights));
    }

    private IncidentCascadeResponse buildCascadeResponse(Incident incident, RiskAnalysis ra) {
        EngineInput input = riskAnalysisService.inputFor(incident);
        SimulationResult sim = cascadeRiskEngine.analyze(input);

        Outcome outcome = toOutcome(sim, input);
        return new IncidentCascadeResponse(
                incident.getIncidentId(),
                incident.getCode(),
                incident.getLocation(),
                ra != null ? ra.getRiskId() : null,
                ra != null ? ra.getAnalysisDate() : null,
                ra != null ? (int) Math.round(ra.getRiskScore()) : null,
                LocalDateTime.now(),
                "SIMULATED",
                outcome,
                Method.standard()
        );
    }

    public static Outcome toOutcome(SimulationResult sim, EngineInput input) {
        List<CascadeNode> nodes = new ArrayList<>();
        List<CascadeNodeDto> simNodes = sim.getNodes();
        for (int i = 0; i < simNodes.size(); i++) {
            CascadeNodeDto n = simNodes.get(i);
            String upstream = i > 0 ? simNodes.get(i - 1).getKey() : null;
            String downstream = i < simNodes.size() - 1 ? simNodes.get(i + 1).getKey() : null;
            nodes.add(new CascadeNode(
                    n.getKey(), n.getLabel(), n.getIcon(),
                    n.getProbability(), n.getSeverity(),
                    n.getLevel().name(), n.getConsequence(),
                    upstream, downstream
            ));
        }

        List<CascadeEdge> edges = List.of(
                new CascadeEdge("rain", "flood", 0.70, 0.70, "Rain → Flood"),
                new CascadeEdge("flood", "road", 0.75, 0.75 * (input.diversion() ? 0.85 : 1.0), "Flood → Road Blockage" + (input.diversion() ? " (traffic diversion active)" : "")),
                new CascadeEdge("road", "traffic", 0.60, 0.60, "Road Blockage → Traffic"),
                new CascadeEdge("traffic", "ambulance", 0.65, 0.65 * (input.greenCorridor() ? 0.70 : 1.0), "Traffic → Ambulance Delay" + (input.greenCorridor() ? " (green corridor active)" : "")),
                new CascadeEdge("ambulance", "hospital", 0.55, 0.55 * (input.hospitalSurge() ? 0.70 : 1.0), "Ambulance Delay → Hospital Impact" + (input.hospitalSurge() ? " (hospital surge active)" : ""))
        );

        List<Factor> factors = sim.getFactors().stream()
                .map(f -> new Factor(f.getKey(), f.getName(), f.getValue(), f.getContribution(), f.getDescription()))
                .toList();

        List<Action> actions = sim.getActions().stream()
                .map(a -> new Action(a.getText(), a.getPriority().name(), a.getTarget()))
                .toList();

        EngineInputs inDto = new EngineInputs(
                input.zone(), input.rainfall(), input.drainage(), input.traffic(),
                input.roadVulnerability(), input.historical(), input.hospitalLoad(),
                input.exposure(), input.pumps(), input.diversion(),
                input.greenCorridor(), input.hospitalSurge()
        );

        return new Outcome(
                sim.getRiskScore(),
                sim.getRiskLevel().name(),
                sim.getZone(),
                sim.getEngine(),
                sim.getExplanation(),
                sim.getBreakPoint(),
                nodes,
                edges,
                factors,
                actions,
                sim.getConsequences(),
                inDto
        );
    }
}
