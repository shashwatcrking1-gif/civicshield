package com.civicshield.repository;

import com.civicshield.model.Incident;
import com.civicshield.model.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {

    long countByStatusNot(IncidentStatus status);

    List<Incident> findByStatusNotOrderByReportedDateDesc(IncidentStatus status);

    List<Incident> findByReportedDateAfterOrderByReportedDateAsc(LocalDateTime after);

    List<Incident> findAllByOrderByReportedDateDesc();
}
