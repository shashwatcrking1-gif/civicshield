package com.civicshield.controller;

import com.civicshield.dto.SimulationResult;
import com.civicshield.model.Incident;
import com.civicshield.model.RiskAnalysis;
import com.civicshield.service.IncidentService;
import com.civicshield.service.RiskAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller for dedicated cascade risk inspection of an incident.
 */
@Controller
@RequiredArgsConstructor
public class RiskController {

    private final IncidentService incidentService;
    private final RiskAnalysisService riskAnalysisService;

    @GetMapping("/portal/risk/{incidentId}")
    public String riskDetail(@PathVariable("incidentId") Long incidentId, Model model) {
        Incident incident = incidentService.get(incidentId);
        RiskAnalysis analysis = riskAnalysisService.latest(incident).orElse(null);
        SimulationResult cascade = riskAnalysisService.evaluate(incident);

        model.addAttribute("incident", incident);
        model.addAttribute("analysis", analysis);
        model.addAttribute("cascade", cascade);
        model.addAttribute("activeNav", "incidents");
        return "portal/risk-detail";
    }

    @GetMapping("/api/risk/{incidentId}")
    @ResponseBody
    public ResponseEntity<SimulationResult> getRiskJson(@PathVariable("incidentId") Long incidentId) {
        Incident incident = incidentService.get(incidentId);
        SimulationResult result = riskAnalysisService.evaluate(incident);
        return ResponseEntity.ok(result);
    }
}
