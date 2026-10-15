package com.civicshield.config;

import com.civicshield.dto.SimulationRequest;
import com.civicshield.model.*;
import com.civicshield.repository.*;
import com.civicshield.service.RiskAnalysisService;
import com.civicshield.service.SimulationService;
import com.civicshield.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds a SIMULATED Chennai monsoon scenario (Velachery, Adyar, T. Nagar, Guindy) on an empty database:
 * 4 demo users (one per role), 12 incidents with risk analyses, alerts and recommendations, 8 data
 * sources and 3 saved simulations. The headline Velachery flood yields the cascade
 * Rain 92% → Flood 81% → Road Blockage 74% → Traffic 69% → Ambulance Delay 58% → Hospital Impact 44%.
 */
@Slf4j
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserService userService;
    private final IncidentRepository incidentRepository;
    private final RiskAnalysisService riskAnalysisService;
    private final AlertRepository alertRepository;
    private final RecommendationRepository recommendationRepository;
    private final DataSourceRepository dataSourceRepository;
    private final SimulationService simulationService;

    public DataSeeder(UserService userService, IncidentRepository incidentRepository,
                      RiskAnalysisService riskAnalysisService, AlertRepository alertRepository,
                      RecommendationRepository recommendationRepository, DataSourceRepository dataSourceRepository,
                      SimulationService simulationService) {
        this.userService = userService;
        this.incidentRepository = incidentRepository;
        this.riskAnalysisService = riskAnalysisService;
        this.alertRepository = alertRepository;
        this.recommendationRepository = recommendationRepository;
        this.dataSourceRepository = dataSourceRepository;
        this.simulationService = simulationService;
    }

    @Value("${civicshield.seed.enabled:true}")
    private boolean enabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled || userService.count() > 0) return;
        log.info("Seeding simulated Chennai demo data…");

        User admin = userService.create("Aarav Menon", "admin@civicshield.in", "Admin@123", Role.ADMIN);
        User authority = userService.create("Priya Raghavan", "authority@civicshield.in", "Authority@123", Role.AUTHORITY);
        User emergency = userService.create("Karthik Subramanian", "emergency@civicshield.in", "Emergency@123", Role.EMERGENCY_SERVICE);
        userService.create("Dr. Meera Iyer", "hospital@civicshield.in", "Hospital@123", Role.HOSPITAL);

        LocalDateTime now = LocalDateTime.now();

        // --- Open incidents (current monsoon spell) ---
        Incident velachery = incident(authority, IncidentType.FLOOD, "Velachery Main Road, near Phoenix Marketcity, Velachery",
                12.9915, 80.2180, Severity.CRITICAL, IncidentStatus.IN_PROGRESS, now.minusHours(2));
        incident(authority, IncidentType.HEAVY_RAIN, "Taramani Link Road, Velachery",
                12.9862, 80.2318, Severity.HIGH, IncidentStatus.VERIFIED, now.minusHours(5));
        incident(emergency, IncidentType.WATERLOGGING, "Lattice Bridge Road, Adyar",
                13.0008, 80.2566, Severity.HIGH, IncidentStatus.REPORTED, now.minusHours(3));
        Incident tree = incident(authority, IncidentType.TREE_FALL, "Gandhi Nagar 2nd Main Road, Adyar",
                13.0071, 80.2545, Severity.MEDIUM, IncidentStatus.IN_PROGRESS, now.minusDays(1).minusHours(2));
        incident(authority, IncidentType.TRAFFIC_CONGESTION, "Usman Road, T. Nagar",
                13.0406, 80.2335, Severity.HIGH, IncidentStatus.VERIFIED, now.minusHours(4));
        incident(emergency, IncidentType.POWER_OUTAGE, "Pondy Bazaar, T. Nagar",
                13.0423, 80.2393, Severity.MEDIUM, IncidentStatus.REPORTED, now.minusHours(6));
        incident(authority, IncidentType.ROAD_BLOCKAGE, "Kathipara Junction, Guindy",
                13.0064, 80.2017, Severity.HIGH, IncidentStatus.IN_PROGRESS, now.minusHours(8));
        incident(admin, IncidentType.DRAINAGE_OVERFLOW, "Industrial Estate Road, Guindy",
                13.0105, 80.2128, Severity.MEDIUM, IncidentStatus.REPORTED, now.minusDays(2).minusHours(3));

        // --- Resolved history ---
        Incident r1 = incident(authority, IncidentType.ROAD_ACCIDENT, "GN Chetty Road, T. Nagar",
                13.0449, 80.2442, Severity.LOW, IncidentStatus.RESOLVED, now.minusDays(4));
        Incident r2 = incident(emergency, IncidentType.MEDICAL_EMERGENCY, "Sardar Patel Road, Adyar",
                13.0060, 80.2432, Severity.MEDIUM, IncidentStatus.RESOLVED, now.minusDays(6));
        Incident r3 = incident(authority, IncidentType.WATERLOGGING, "Velachery Bypass Road, Velachery",
                12.9785, 80.2212, Severity.MEDIUM, IncidentStatus.RESOLVED, now.minusDays(9));
        Incident r4 = incident(admin, IncidentType.FIRE, "SIDCO Industrial Estate, Guindy",
                13.0131, 80.2081, Severity.LOW, IncidentStatus.RESOLVED, now.minusDays(12));

        // Analyse every incident a few minutes after it was reported
        for (Incident i : incidentRepository.findAll()) {
            riskAnalysisService.analyzeAndStore(i, i.getReportedDate().plusMinutes(3));
        }
        alertRepository.flush();

        // Close the loop on resolved incidents; mark some open alerts as acknowledged
        List<Long> resolved = List.of(r1.getIncidentId(), r2.getIncidentId(), r3.getIncidentId(), r4.getIncidentId());
        alertRepository.findAll().forEach(a -> {
            Long incidentId = a.getRiskAnalysis().getIncident().getIncidentId();
            if (resolved.contains(incidentId)) a.setAlertStatus(AlertStatus.RESOLVED);
            else if (incidentId.equals(tree.getIncidentId())) a.setAlertStatus(AlertStatus.ACKNOWLEDGED);
        });
        recommendationRepository.findAll().forEach(r -> {
            Long incidentId = r.getRiskAnalysis().getIncident().getIncidentId();
            if (resolved.contains(incidentId)) r.setStatus(RecommendationStatus.APPROVED);
            else if (incidentId.equals(tree.getIncidentId()) && r.getPriority() == Priority.LOW)
                r.setStatus(RecommendationStatus.DISMISSED);
        });

        // --- Data sources (simulated integrations) ---
        dataSourceRepository.saveAll(List.of(
                ds("IMD Chennai Rainfall Nowcast", "Weather API", "Nungambakkam & Meenambakkam", DataStatus.ONLINE),
                ds("GCC Storm-Water Drain Level Sensors", "IoT Sensor", "Velachery, Adyar", DataStatus.DELAYED),
                ds("Adyar River Level Gauge", "IoT Sensor", "Kotturpuram Bridge", DataStatus.ONLINE),
                ds("Greater Chennai Traffic Police Signal Feed", "Traffic API", "City-wide", DataStatus.ONLINE),
                ds("108 Ambulance Dispatch Feed", "Emergency Services", "Chennai District", DataStatus.ONLINE),
                ds("Hospital Bed Availability Registry", "Health Registry", "Govt. & private hospitals", DataStatus.DELAYED),
                ds("TANGEDCO Outage Reports", "Utility Feed", "T. Nagar, Guindy", DataStatus.OFFLINE),
                ds("Citizen Reports – Civic Mobile App", "Crowdsourced", "City-wide", DataStatus.ONLINE)));

        // --- Saved What-If scenarios ---
        simulationService.saveScenario("Velachery cloudburst – pumps + diversion",
                SimulationRequest.builder().zone("Velachery").rainfall(115.0).traffic(82.0).drainage(35.0)
                        .pumps(true).diversion(true).build(), authority);
        simulationService.saveScenario("T. Nagar evening peak with moderate rain",
                SimulationRequest.builder().zone("T. Nagar").rainfall(60.0).traffic(90.0).drainage(50.0)
                        .diversion(true).greenCorridor(true).build(), authority);
        simulationService.saveScenario("Adyar river surge – hospital readiness",
                SimulationRequest.builder().zone("Adyar").rainfall(140.0).traffic(64.0).drainage(45.0)
                        .greenCorridor(true).hospitalSurge(true).build(), admin);

        log.info("Seeded {} incidents. Headline cascade at {}.", incidentRepository.count(), velachery.getLocation());
    }

    private Incident incident(User by, IncidentType type, String location, double lat, double lng,
                              Severity severity, IncidentStatus status, LocalDateTime at) {
        return incidentRepository.save(Incident.builder().reportedBy(by).incidentType(type).location(location)
                .latitude(lat).longitude(lng).severity(severity).status(status).reportedDate(at).build());
    }

    private DataSource ds(String name, String type, String location, DataStatus status) {
        return DataSource.builder().sourceName(name).sourceType(type).location(location).dataStatus(status).build();
    }
}
