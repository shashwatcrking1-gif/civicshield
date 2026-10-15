package com.civicshield.service;

import com.civicshield.dto.SimulationRequest;
import com.civicshield.dto.SimulationResult;
import com.civicshield.exception.ResourceNotFoundException;
import com.civicshield.model.Simulation;
import com.civicshield.model.User;
import com.civicshield.repository.SimulationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** What-If simulations for the public demo and the authority portal. */
@Service
public class SimulationService {

    private final AiClientService aiClientService;
    private final CascadeRiskEngine engine;
    private final SimulationRepository repository;

    public SimulationService(AiClientService aiClientService, CascadeRiskEngine engine, SimulationRepository repository) {
        this.aiClientService = aiClientService;
        this.engine = engine;
        this.repository = repository;
    }

    /** Public demo – no persistence, interventions ignored. */
    public SimulationResult runPublic(SimulationRequest request) {
        request.setPumps(false);
        request.setDiversion(false);
        request.setGreenCorridor(false);
        request.setHospitalSurge(false);
        request.setSave(false);
        return aiClientService.simulate(request);
    }

    /**
     * Portal run: BEFORE (no interventions) vs AFTER (selected interventions), a risk-vs-rainfall curve
     * for both, and optional persistence of the scenario.
     */
    @Transactional
    public Map<String, Object> runPortal(SimulationRequest request, User user) {
        SimulationRequest baseline = copy(request);
        baseline.setPumps(false);
        baseline.setDiversion(false);
        baseline.setGreenCorridor(false);
        baseline.setHospitalSurge(false);

        SimulationResult before = aiClientService.simulate(baseline);
        SimulationResult after = engine.simulate(request);
        after.setEngine(before.getEngine());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("before", before);
        body.put("after", after);
        body.put("curve", curve(request));
        body.put("interventions", interventionLabels(request));

        if (request.isSave()) {
            Simulation saved = repository.save(Simulation.builder()
                    .createdBy(user)
                    .scenarioName(request.getScenarioName() == null || request.getScenarioName().isBlank()
                            ? "Untitled scenario – " + before.getZone() : request.getScenarioName().trim())
                    .rainfallLevel(request.getRainfall())
                    .trafficLevel(request.getTraffic())
                    .predictedRisk((double) before.getRiskScore())
                    .result(summary(request, before, after))
                    .build());
            body.put("savedId", saved.getSimulationId());
        }
        return body;
    }

    /** Risk score for rainfall 0..200 mm/hr, with and without interventions. */
    public Map<String, List<Integer>> curve(SimulationRequest r) {
        List<Integer> labels = new ArrayList<>(), before = new ArrayList<>(), after = new ArrayList<>();
        for (int rain = 0; rain <= 200; rain += 10) {
            SimulationRequest b = copy(r);
            b.setRainfall((double) rain);
            SimulationRequest a = copy(b);
            b.setPumps(false);
            b.setDiversion(false);
            b.setGreenCorridor(false);
            b.setHospitalSurge(false);
            labels.add(rain);
            before.add(engine.score(engine.inputFor(b)));
            after.add(engine.score(engine.inputFor(a)));
        }
        Map<String, List<Integer>> m = new LinkedHashMap<>();
        m.put("labels", labels);
        m.put("before", before);
        m.put("after", after);
        return m;
    }

    public List<Simulation> saved() {
        return repository.findAllByOrderBySimulationIdDesc();
    }

    public long count() {
        return repository.count();
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) throw new ResourceNotFoundException("Simulation", id);
        repository.deleteById(id);
    }

    /** Persists a scenario directly (used by the demo data seeder). */
    @Transactional
    public Simulation saveScenario(String name, SimulationRequest r, User user) {
        r.setScenarioName(name);
        r.setSave(true);
        SimulationRequest baseline = copy(r);
        baseline.setPumps(false);
        baseline.setDiversion(false);
        baseline.setGreenCorridor(false);
        baseline.setHospitalSurge(false);
        SimulationResult before = engine.simulate(baseline);
        SimulationResult after = engine.simulate(r);
        return repository.save(Simulation.builder().createdBy(user).scenarioName(name)
                .rainfallLevel(r.getRainfall()).trafficLevel(r.getTraffic())
                .predictedRisk((double) before.getRiskScore())
                .result(summary(r, before, after)).build());
    }

    private String summary(SimulationRequest r, SimulationResult before, SimulationResult after) {
        List<String> iv = interventionLabels(r);
        String s = String.format("%s · drainage %.0f%% · Before %d (%s) → After %d (%s)%s",
                before.getZone(), r.getDrainage(), before.getRiskScore(), before.getRiskLevel().getLabel(),
                after.getRiskScore(), after.getRiskLevel().getLabel(),
                iv.isEmpty() ? " · no interventions" : " · " + String.join(", ", iv));
        return s.length() > 1000 ? s.substring(0, 1000) : s;
    }

    private List<String> interventionLabels(SimulationRequest r) {
        List<String> l = new ArrayList<>();
        if (r.isPumps()) l.add("Mobile pumps");
        if (r.isDiversion()) l.add("Traffic diversion");
        if (r.isGreenCorridor()) l.add("Green corridor");
        if (r.isHospitalSurge()) l.add("Hospital surge");
        return l;
    }

    private static SimulationRequest copy(SimulationRequest r) {
        return SimulationRequest.builder()
                .scenarioName(r.getScenarioName()).zone(r.getZone())
                .rainfall(r.getRainfall()).traffic(r.getTraffic()).drainage(r.getDrainage())
                .pumps(r.isPumps()).diversion(r.isDiversion())
                .greenCorridor(r.isGreenCorridor()).hospitalSurge(r.isHospitalSurge())
                .save(r.isSave()).build();
    }
}
