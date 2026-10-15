package com.civicshield.controller;

import com.civicshield.dto.IncidentForm;
import com.civicshield.dto.SimulationResult;
import com.civicshield.model.*;
import com.civicshield.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * Controller for listing, creating, inspecting and updating incidents.
 */
@Controller
@RequestMapping("/portal/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;
    private final RiskAnalysisService riskAnalysisService;
    private final AlertService alertService;
    private final RecommendationService recommendationService;
    private final UserService userService;
    private final CascadeRiskEngine engine;

    @GetMapping
    public String listIncidents(@RequestParam(value = "q", required = false) String q,
                                @RequestParam(value = "type", required = false) IncidentType type,
                                @RequestParam(value = "severity", required = false) Severity severity,
                                @RequestParam(value = "status", required = false) IncidentStatus status,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                Model model) {
        int pageSize = 10;
        Page<Incident> incidentPage = incidentService.search(q, type, severity, status, page, pageSize);
        Map<Long, RiskAnalysis> latestAnalyses = riskAnalysisService.latestByIncident();

        Map<String, Object> queryParams = new java.util.HashMap<>();
        queryParams.put("q", q != null ? q : "");
        queryParams.put("type", type != null ? type.name() : "");
        queryParams.put("severity", severity != null ? severity.name() : "");
        queryParams.put("status", status != null ? status.name() : "");
        model.addAttribute("queryParams", queryParams);

        model.addAttribute("incidentPage", incidentPage);
        model.addAttribute("latestAnalyses", latestAnalyses);
        model.addAttribute("q", q);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedSeverity", severity);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("incidentTypes", IncidentType.values());
        model.addAttribute("severities", Severity.values());
        model.addAttribute("statuses", IncidentStatus.values());
        model.addAttribute("activeNav", "incidents");
        return "portal/incidents";
    }

    @GetMapping("/new")
    public String newIncidentForm(Model model) {
        if (!model.containsAttribute("incidentForm")) {
            IncidentForm form = new IncidentForm();
            // Default location: Velachery Main Road
            form.setLocation("Velachery Main Road, Chennai");
            form.setLatitude(12.9815);
            form.setLongitude(80.2180);
            form.setSeverity(Severity.HIGH);
            form.setIncidentType(IncidentType.FLOOD);
            model.addAttribute("incidentForm", form);
        }
        model.addAttribute("incidentTypes", IncidentType.values());
        model.addAttribute("severities", Severity.values());
        model.addAttribute("zones", engine.zones());
        model.addAttribute("activeNav", "incidents");
        return "portal/incident-new";
    }

    @PostMapping("/new")
    public String createIncident(@Valid @ModelAttribute("incidentForm") IncidentForm form,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.incidentForm", bindingResult);
            redirectAttributes.addFlashAttribute("incidentForm", form);
            return "redirect:/portal/incidents/new";
        }
        User reporter = userDetails != null ? userService.findByEmail(userDetails.getUsername()).orElse(null) : null;
        Incident created = incidentService.create(form, reporter);
        redirectAttributes.addFlashAttribute("successMessage",
                "Incident " + created.getCode() + " logged successfully. Cascade risk engine initiated.");
        return "redirect:/portal/incidents/" + created.getIncidentId();
    }

    @GetMapping("/{id}")
    public String viewIncident(@PathVariable("id") Long id,
                               @RequestParam(value = "tab", defaultValue = "risk") String tab,
                               Model model) {
        Incident incident = incidentService.get(id);
        RiskAnalysis analysis = riskAnalysisService.latest(incident).orElse(null);
        SimulationResult cascade = riskAnalysisService.evaluate(incident);
        List<Alert> alerts = alertService.forIncident(id);
        List<Recommendation> recommendations = recommendationService.forIncident(id);

        model.addAttribute("incident", incident);
        model.addAttribute("analysis", analysis);
        model.addAttribute("cascade", cascade);
        model.addAttribute("alerts", alerts);
        model.addAttribute("recommendations", recommendations);
        model.addAttribute("activeTab", tab);
        model.addAttribute("statuses", IncidentStatus.values());
        model.addAttribute("activeNav", "incidents");
        return "portal/incident-detail";
    }

    @PostMapping("/{id}/status")
    public String updateIncidentStatus(@PathVariable("id") Long id,
                                       @RequestParam("status") IncidentStatus status,
                                       RedirectAttributes redirectAttributes) {
        Incident updated = incidentService.updateStatus(id, status);
        redirectAttributes.addFlashAttribute("successMessage",
                "Incident " + updated.getCode() + " status updated to " + status.getLabel() + ".");
        return "redirect:/portal/incidents/" + id + "?tab=timeline";
    }

    @PostMapping("/{id}/reanalyze")
    public String reanalyzeIncident(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        incidentService.reanalyze(id);
        redirectAttributes.addFlashAttribute("successMessage",
                "Cascade engine re-analyzed incident #" + id + " with real-time zone parameters.");
        return "redirect:/portal/incidents/" + id + "?tab=risk";
    }
}
