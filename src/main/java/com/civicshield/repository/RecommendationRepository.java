package com.civicshield.repository;

import com.civicshield.model.Recommendation;
import com.civicshield.model.RecommendationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    List<Recommendation> findByStatusOrderByRecommendationIdDesc(RecommendationStatus status);

    List<Recommendation> findAllByOrderByRecommendationIdDesc();

    List<Recommendation> findByRiskAnalysis_Incident_IncidentIdOrderByRecommendationIdAsc(Long incidentId);

    long countByStatus(RecommendationStatus status);
}
