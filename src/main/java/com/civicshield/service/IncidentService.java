package com.civicshield.service;

import com.civicshield.dto.IncidentForm;
import com.civicshield.exception.ResourceNotFoundException;
import com.civicshield.model.*;
import com.civicshield.repository.IncidentRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Incident CRUD, search and status workflow. */
@Service
public class IncidentService {

    private final IncidentRepository repository;
    private final RiskAnalysisService riskAnalysisService;
    private final AlertService alertService;

    public IncidentService(IncidentRepository repository, RiskAnalysisService riskAnalysisService, AlertService alertService) {
        this.repository = repository;
        this.riskAnalysisService = riskAnalysisService;
        this.alertService = alertService;
    }

    public Incident get(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Incident", id));
    }

    public List<Incident> all() {
        return repository.findAllByOrderByReportedDateDesc();
    }

    public List<Incident> open() {
        return repository.findByStatusNotOrderByReportedDateDesc(IncidentStatus.RESOLVED);
    }

    public long activeCount() {
        return repository.countByStatusNot(IncidentStatus.RESOLVED);
    }

    /**
     * Searchable, filterable, paginated list. {@code q} matches location text or an incident code
     * such as "INC-0001" / "1".
     */
    public Page<Incident> search(String q, IncidentType type, Severity severity, IncidentStatus status,
                                 int page, int size) {
        Specification<Incident> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String term = q.trim().toLowerCase(Locale.ROOT);
                Long id = parseCode(term);
                Predicate byLocation = cb.like(cb.lower(root.get("location")), "%" + term + "%");
                p.add(id != null ? cb.or(byLocation, cb.equal(root.get("incidentId"), id)) : byLocation);
            }
            if (type != null) p.add(cb.equal(root.get("incidentType"), type));
            if (severity != null) p.add(cb.equal(root.get("severity"), severity));
            if (status != null) p.add(cb.equal(root.get("status"), status));
            return cb.and(p.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Direction.DESC, "reportedDate")));
    }

    /** Saves a new incident and immediately runs the cascade analysis. */
    @Transactional
    public Incident create(IncidentForm form, User reporter) {
        Incident incident = repository.save(Incident.builder()
                .reportedBy(reporter)
                .incidentType(form.getIncidentType())
                .location(form.getLocation().trim())
                .latitude(form.getLatitude())
                .longitude(form.getLongitude())
                .severity(form.getSeverity())
                .status(IncidentStatus.REPORTED)
                .reportedDate(LocalDateTime.now())
                .build());
        riskAnalysisService.analyzeAndStore(incident, LocalDateTime.now());
        return incident;
    }

    @Transactional
    public Incident updateStatus(Long id, IncidentStatus status) {
        Incident incident = get(id);
        incident.setStatus(status);
        if (status == IncidentStatus.RESOLVED) alertService.resolveForIncident(id);
        return incident;
    }

    /** Re-runs the engine for an incident and stores a fresh analysis. */
    @Transactional
    public void reanalyze(Long id) {
        riskAnalysisService.analyzeAndStore(get(id), LocalDateTime.now());
    }

    static Long parseCode(String term) {
        String digits = term.replace("inc-", "").replace("inc", "").trim();
        if (digits.isEmpty() || !digits.chars().allMatch(Character::isDigit) || digits.length() > 9) return null;
        return Long.parseLong(digits);
    }
}
