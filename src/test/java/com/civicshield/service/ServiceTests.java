package com.civicshield.service;

import com.civicshield.dto.IncidentForm;
import com.civicshield.dto.SimulationRequest;
import com.civicshield.model.*;
import com.civicshield.repository.IncidentRepository;
import com.civicshield.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("demo")
@Transactional
class ServiceTests {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private SimulationService simulationService;

    @Autowired
    private UserService userService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Test
    void testIncidentLifecycle() {
        User admin = userRepository.findByEmailIgnoreCase("admin@civicshield.in").orElseThrow();

        IncidentForm form = new IncidentForm();
        form.setIncidentType(IncidentType.FLOOD);
        form.setLocation("South Usman Road, T. Nagar");
        form.setLatitude(13.0380);
        form.setLongitude(80.2330);
        form.setSeverity(Severity.HIGH);

        Incident created = incidentService.create(form, admin);
        assertNotNull(created.getIncidentId());
        assertEquals(IncidentStatus.REPORTED, created.getStatus());

        // Update status to IN_PROGRESS
        Incident inProgress = incidentService.updateStatus(created.getIncidentId(), IncidentStatus.IN_PROGRESS);
        assertEquals(IncidentStatus.IN_PROGRESS, inProgress.getStatus());

        // Check alerts were created for this incident
        List<Alert> alerts = alertService.forIncident(created.getIncidentId());
        assertFalse(alerts.isEmpty(), "Alerts should be created for high severity incident");

        // Resolve incident - alerts should resolve
        incidentService.updateStatus(created.getIncidentId(), IncidentStatus.RESOLVED);
        List<Alert> resolvedAlerts = alertService.forIncident(created.getIncidentId());
        for (Alert a : resolvedAlerts) {
            assertEquals(AlertStatus.RESOLVED, a.getAlertStatus());
        }
    }

    @Test
    void testAlertAndRecommendationActions() {
        var alerts = alertService.byStatus(AlertStatus.ACTIVE);
        if (!alerts.isEmpty()) {
            Alert a = alerts.get(0);
            Alert ack = alertService.acknowledge(a.getAlertId());
            assertEquals(AlertStatus.ACKNOWLEDGED, ack.getAlertStatus());

            Alert res = alertService.resolve(a.getAlertId());
            assertEquals(AlertStatus.RESOLVED, res.getAlertStatus());
        }

        var recs = recommendationService.list(RecommendationStatus.PENDING);
        if (!recs.isEmpty()) {
            Recommendation r = recs.get(0);
            Recommendation app = recommendationService.approve(r.getRecommendationId());
            assertEquals(RecommendationStatus.APPROVED, app.getStatus());
        }
    }

    @Test
    void testSimulationService() {
        User admin = userRepository.findByEmailIgnoreCase("admin@civicshield.in").orElseThrow();
        SimulationRequest request = SimulationRequest.builder()
                .scenarioName("Test Scenario")
                .zone("Adyar")
                .rainfall(70.0)
                .traffic(60.0)
                .drainage(45.0)
                .pumps(true)
                .save(true)
                .build();

        Map<String, Object> result = simulationService.runPortal(request, admin);
        assertNotNull(result.get("before"));
        assertNotNull(result.get("after"));
        assertNotNull(result.get("curve"));
        assertNotNull(result.get("savedId"));

        Long savedId = (Long) result.get("savedId");
        assertTrue(simulationService.count() > 0);

        simulationService.delete(savedId);
    }

    @Test
    void testUserServiceAndRules() {
        User user = userService.create("Officer Sharma", "sharma@civicshield.in", "Password@123", Role.AUTHORITY);
        assertNotNull(user.getUserId());
        assertTrue(user.isEnabled());

        // Duplicate email error
        var form = userService.toForm(user);
        form.setUserId(null); // treat as new
        String error = userService.save(form);
        assertNotNull(error, "Duplicate email should return error message");
    }

    @Test
    void testReportServiceCsv() {
        var rows = reportService.rows(null, null, null, null, null);
        assertFalse(rows.isEmpty());

        String csv = reportService.toCsv(rows);
        assertNotNull(csv);
        assertTrue(csv.contains("Incident ID,Type,Location"));
    }
}
