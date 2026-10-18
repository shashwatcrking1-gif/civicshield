package com.civicshield.service;

import com.civicshield.dto.SimulationRequest;
import com.civicshield.dto.SimulationResult;
import com.civicshield.model.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CascadeRiskEngineTest {

    private CascadeRiskEngine engine;

    @BeforeEach
    void setUp() {
        engine = new CascadeRiskEngine();
    }

    @Test
    void testStandardMonsoonSimulation() {
        SimulationRequest request = SimulationRequest.builder()
                .zone("Velachery")
                .rainfall(120.0)
                .traffic(80.0)
                .drainage(30.0)
                .build();

        SimulationResult result = engine.simulate(request);

        assertNotNull(result);
        assertTrue(result.getRiskScore() >= 60, "Expected high/critical risk score for 120mm rain in Velachery");
        assertEquals("Velachery", result.getZone());
        assertEquals("JAVA_CASCADE_ENGINE", result.getEngine());
        assertEquals(6, result.getNodes().size(), "Expected 6 cascade nodes");

        // Verify chain order
        assertEquals("rain", result.getNodes().get(0).getKey());
        assertEquals("flood", result.getNodes().get(1).getKey());
        assertEquals("road", result.getNodes().get(2).getKey());
        assertEquals("traffic", result.getNodes().get(3).getKey());
        assertEquals("ambulance", result.getNodes().get(4).getKey());
        assertEquals("hospital", result.getNodes().get(5).getKey());

        // Verify probabilities
        assertTrue(result.getNodes().get(0).getProbability() > 80);
        assertTrue(result.getNodes().get(1).getProbability() > 70);

        // Verify factors and explanation
        assertFalse(result.getFactors().isEmpty());
        assertNotNull(result.getExplanation());
        assertNotNull(result.getBreakPoint());
        assertFalse(result.getActions().isEmpty());
    }

    @Test
    void testInterventionsReduceRisk() {
        SimulationRequest baseline = SimulationRequest.builder()
                .zone("Velachery")
                .rainfall(100.0)
                .traffic(75.0)
                .drainage(35.0)
                .build();

        SimulationResult baselineResult = engine.simulate(baseline);

        SimulationRequest mitigated = SimulationRequest.builder()
                .zone("Velachery")
                .rainfall(100.0)
                .traffic(75.0)
                .drainage(35.0)
                .pumps(true)
                .diversion(true)
                .greenCorridor(true)
                .hospitalSurge(true)
                .build();

        SimulationResult mitigatedResult = engine.simulate(mitigated);

        assertTrue(mitigatedResult.getRiskScore() < baselineResult.getRiskScore(),
                "Interventions should strictly lower total cascade risk");
        assertTrue(mitigatedResult.node("flood").getProbability() <= baselineResult.node("flood").getProbability(),
                "Pumps should lower or maintain flood probability");
    }

    @Test
    void testZoneResolution() {
        var velachery = engine.zone("Velachery");
        assertEquals("Velachery", velachery.name());

        var adyar = engine.zoneForLocation("Lattice Bridge Road, Adyar, Chennai");
        assertEquals("Adyar", adyar.name());

        var fallback = engine.zoneForLocation("Random Place");
        assertNotNull(fallback);
    }
}
