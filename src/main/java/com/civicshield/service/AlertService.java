package com.civicshield.service;

import com.civicshield.dto.CascadeNodeDto;
import com.civicshield.dto.SimulationResult;
import com.civicshield.exception.ResourceNotFoundException;
import com.civicshield.model.*;
import com.civicshield.repository.AlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Creates alerts from risk analyses and manages their Active → Acknowledged → Resolved lifecycle. */
@Service
public class AlertService {

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public List<Alert> byStatus(AlertStatus status) {
        return alertRepository.findByAlertStatusOrderByAlertDateDesc(status);
    }

    public List<Alert> recentOpen() {
        return alertRepository.findTop5ByAlertStatusNotOrderByAlertDateDesc(AlertStatus.RESOLVED);
    }

    public List<Alert> forIncident(Long incidentId) {
        return alertRepository.findByRiskAnalysis_Incident_IncidentIdOrderByAlertDateDesc(incidentId);
    }

    public Map<AlertStatus, Long> counts() {
        Map<AlertStatus, Long> m = new EnumMap<>(AlertStatus.class);
        for (AlertStatus s : AlertStatus.values()) m.put(s, alertRepository.countByAlertStatus(s));
        return m;
    }

    public long activeCount() {
        return alertRepository.countByAlertStatus(AlertStatus.ACTIVE);
    }

    public long criticalActiveCount() {
        return alertRepository.countByAlertStatusAndPriority(AlertStatus.ACTIVE, Priority.CRITICAL);
    }

    @Transactional
    public Alert acknowledge(Long id) {
        Alert a = get(id);
        if (a.getAlertStatus() == AlertStatus.ACTIVE) a.setAlertStatus(AlertStatus.ACKNOWLEDGED);
        return a;
    }

    @Transactional
    public Alert resolve(Long id) {
        Alert a = get(id);
        a.setAlertStatus(AlertStatus.RESOLVED);
        return a;
    }

    /** Resolves every open alert of an incident (called when the incident is closed). */
    @Transactional
    public void resolveForIncident(Long incidentId) {
        forIncident(incidentId).forEach(a -> a.setAlertStatus(AlertStatus.RESOLVED));
    }

    /**
     * Raises alerts for Medium+ risk: one cascade alert, plus a dedicated ambulance-delay alert when
     * emergency response is likely to be affected.
     */
    @Transactional
    public void createForAnalysis(RiskAnalysis ra, SimulationResult r, LocalDateTime at) {
        if (r.getRiskLevel() == RiskLevel.LOW) return;
        Incident i = ra.getIncident();
        String firstAction = r.getActions().isEmpty() ? "" : " " + r.getActions().get(0).getText() + ".";

        alertRepository.save(Alert.builder()
                .riskAnalysis(ra)
                .message(String.format("%s cascade risk at %s: %s.%s", r.getRiskLevel().getLabel(),
                        i.getLocation(), r.chainSummary(3), firstAction))
                .priority(Priority.fromRiskLevel(r.getRiskLevel()))
                .alertStatus(AlertStatus.ACTIVE)
                .alertDate(at)
                .build());

        CascadeNodeDto amb = r.node("ambulance");
        if (amb != null && amb.getProbability() >= 45) {
            alertRepository.save(Alert.builder()
                    .riskAnalysis(ra)
                    .message(String.format("Ambulance delay likely (%d%%) around %s – coordinate 108 dispatch and hospital intake.",
                            amb.getProbability(), r.getZone()))
                    .priority(Priority.fromProbability(amb.getProbability()))
                    .alertStatus(AlertStatus.ACTIVE)
                    .alertDate(at.plusMinutes(1))
                    .build());
        }
    }

    private Alert get(Long id) {
        return alertRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Alert", id));
    }
}
