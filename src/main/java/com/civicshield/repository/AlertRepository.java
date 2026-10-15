package com.civicshield.repository;

import com.civicshield.model.Alert;
import com.civicshield.model.AlertStatus;
import com.civicshield.model.Priority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByAlertStatusOrderByAlertDateDesc(AlertStatus status);

    List<Alert> findTop5ByAlertStatusNotOrderByAlertDateDesc(AlertStatus status);

    List<Alert> findByRiskAnalysis_Incident_IncidentIdOrderByAlertDateDesc(Long incidentId);

    long countByAlertStatus(AlertStatus status);

    long countByAlertStatusAndPriority(AlertStatus status, Priority priority);
}
