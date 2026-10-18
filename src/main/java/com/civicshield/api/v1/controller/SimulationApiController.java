package com.civicshield.api.v1.controller;

import com.civicshield.api.v1.auth.TokenService;
import com.civicshield.api.v1.dto.CascadeDtos.Outcome;
import com.civicshield.api.v1.dto.PageResponse;
import com.civicshield.api.v1.dto.SimulationReportDtos.SimulationParametersDto;
import com.civicshield.api.v1.dto.SimulationReportDtos.SimulationRecordDto;
import com.civicshield.api.v1.dto.SimulationReportDtos.SimulationRunResponse;
import com.civicshield.dto.SimulationRequest;
import com.civicshield.dto.SimulationResult;
import com.civicshield.exception.ApiException;
import com.civicshield.model.Simulation;
import com.civicshield.model.User;
import com.civicshield.repository.SimulationRepository;
import com.civicshield.repository.UserRepository;
import com.civicshield.service.CascadeRiskEngine;
import com.civicshield.service.CascadeRiskEngine.EngineInput;
import com.civicshield.service.CascadeRiskEngine.ZoneProfile;
import com.civicshield.service.SimulationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/simulations")
public class SimulationApiController {

    private final SimulationService simulationService;
    private final CascadeRiskEngine cascadeRiskEngine;
    private final SimulationRepository simulationRepository;
    private final UserRepository userRepository;

    public SimulationApiController(SimulationService simulationService,
                                   CascadeRiskEngine cascadeRiskEngine,
                                   SimulationRepository simulationRepository,
                                   UserRepository userRepository) {
        this.simulationService = simulationService;
        this.cascadeRiskEngine = cascadeRiskEngine;
        this.simulationRepository = simulationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/parameters")
    public ResponseEntity<SimulationParametersDto> parameters() {
        List<Map<String, Object>> parameters = List.of(
                Map.of("key", "rainfall", "label", "Rainfall intensity", "unit", "mm/hr", "min", 0, "max", 200, "step", 1),
                Map.of("key", "traffic", "label", "Traffic density", "unit", "% of peak", "min", 0, "max", 100, "step", 1),
                Map.of("key", "drainage", "label", "Drainage capacity", "unit", "% effective", "min", 0, "max", 100, "step", 1)
        );

        List<Map<String, Object>> interventions = List.of(
                Map.of("key", "pumps", "label", "Mobile pumps", "effect", "+25 points drainage capacity"),
                Map.of("key", "diversion", "label", "Traffic diversion", "effect", "−20 points traffic; Flood→Road weight ×0.85"),
                Map.of("key", "greenCorridor", "label", "Green corridor", "effect", "Traffic→Ambulance weight ×0.70"),
                Map.of("key", "hospitalSurge", "label", "Hospital surge protocol", "effect", "Ambulance→Hospital weight ×0.70")
        );

        List<Map<String, Object>> zones = cascadeRiskEngine.zones().stream().map(z -> Map.<String, Object>of(
                "name", z.name(),
                "defaultDrainage", z.drainage(),
                "defaultTraffic", z.baseTraffic()
        )).toList();

        return ResponseEntity.ok(new SimulationParametersDto(parameters, interventions, zones));
    }

    @PostMapping("/run")
    public ResponseEntity<SimulationRunResponse> run(@Valid @RequestBody SimulationRequest req, Authentication authentication) {
        if (req.getRainfall() == null || req.getTraffic() == null || req.getDrainage() == null) {
            throw ApiException.badRequest("Rainfall, traffic, and drainage are required");
        }

        EngineInput baselineInput = cascadeRiskEngine.inputFor(copyBaseline(req));
        SimulationResult baselineResult = cascadeRiskEngine.analyze(baselineInput);

        EngineInput scenarioInput = cascadeRiskEngine.inputFor(req);
        SimulationResult scenarioResult = cascadeRiskEngine.analyze(scenarioInput);

        Outcome baselineOutcome = RiskApiController.toOutcome(baselineResult, baselineInput);
        Outcome scenarioOutcome = RiskApiController.toOutcome(scenarioResult, scenarioInput);

        Map<String, List<Integer>> rawCurve = simulationService.curve(req);
        Map<String, List<Integer>> curve = new LinkedHashMap<>();
        curve.put("rainfall", rawCurve.get("labels"));
        curve.put("baseline", rawCurve.get("before"));
        curve.put("scenario", rawCurve.get("after"));
        curve.put("labels", rawCurve.get("labels"));
        curve.put("before", rawCurve.get("before"));
        curve.put("after", rawCurve.get("after"));

        List<String> interventions = new ArrayList<>();
        if (req.isPumps()) interventions.add("Mobile pumps");
        if (req.isDiversion()) interventions.add("Traffic diversion");
        if (req.isGreenCorridor()) interventions.add("Green corridor");
        if (req.isHospitalSurge()) interventions.add("Hospital surge");

        List<String> assumptions = List.of(
                "Zone profile values (road vulnerability, history, hospital load, exposure) are simulated",
                "Rainfall intensity corresponds to constant cloudburst conditions over a 1-hour window",
                "Interventions are assumed to deploy with 100% operational fidelity"
        );

        Long savedId = null;
        if (req.isSave()) {
            User user = resolveUser(authentication);
            Simulation saved = simulationService.saveScenario(
                    req.getScenarioName() != null && !req.getScenarioName().isBlank() ? req.getScenarioName().trim() : "Scenario – " + scenarioResult.getZone(),
                    req,
                    user
            );
            savedId = saved.getSimulationId();
        }

        return ResponseEntity.ok(new SimulationRunResponse(
                "HYPOTHETICAL",
                baselineOutcome,
                scenarioOutcome,
                curve,
                interventions,
                assumptions,
                savedId
        ));
    }

    @GetMapping
    public ResponseEntity<PageResponse<SimulationRecordDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int validSize = Math.min(Math.max(1, size), 100);
        List<Simulation> all = simulationRepository.findAllByOrderBySimulationIdDesc();
        List<SimulationRecordDto> dtos = all.stream().map(s -> new SimulationRecordDto(
                s.getSimulationId(),
                s.getScenarioName(),
                s.getRainfallLevel(),
                s.getTrafficLevel(),
                s.getPredictedRisk(),
                s.getResult(),
                s.getCreatedBy() != null ? s.getCreatedBy().getName() : "Anonymous"
        )).toList();

        return ResponseEntity.ok(PageResponse.of(dtos, page, validSize));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        simulationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private SimulationRequest copyBaseline(SimulationRequest r) {
        return SimulationRequest.builder()
                .scenarioName(r.getScenarioName())
                .zone(r.getZone())
                .rainfall(r.getRainfall())
                .traffic(r.getTraffic())
                .drainage(r.getDrainage())
                .pumps(false)
                .diversion(false)
                .greenCorridor(false)
                .hospitalSurge(false)
                .save(false)
                .build();
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null) return null;
        String email = authentication.getName();
        if (authentication.getPrincipal() instanceof TokenService.TokenInfo info) {
            email = info.email();
        }
        return userRepository.findByEmailIgnoreCase(email).orElse(null);
    }
}
