package com.civicshield.repository;

import com.civicshield.model.Incident;
import com.civicshield.model.RiskAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RiskAnalysisRepository extends JpaRepository<RiskAnalysis, Long> {

    Optional<RiskAnalysis> findTopByIncidentOrderByAnalysisDateDesc(Incident incident);

    List<RiskAnalysis> findAllByOrderByAnalysisDateDesc();
    List<RiskAnalysis> findByIncident_IncidentIdOrderByAnalysisDateDesc(Long incidentId);
}
