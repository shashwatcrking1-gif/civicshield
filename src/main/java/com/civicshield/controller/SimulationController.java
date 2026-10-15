package com.civicshield.controller;

import com.civicshield.dto.SimulationRequest;
import com.civicshield.model.Simulation;
import com.civicshield.model.User;
import com.civicshield.service.CascadeRiskEngine;
import com.civicshield.service.SimulationService;
import com.civicshield.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * Controller for the full What-If Simulator in the portal (BEFORE vs AFTER, curves, saved scenarios).
 */
@Controller
public class SimulationController {

    private final SimulationService simulationService;
    private final CascadeRiskEngine engine;
    private final UserService userService;

    public SimulationController(SimulationService simulationService, CascadeRiskEngine engine, UserService userService) {
        this.simulationService = simulationService;
        this.engine = engine;
        this.userService = userService;
    }

    @GetMapping("/portal/simulator")
    public String simulatorPage(Model model) {
        model.addAttribute("zones", engine.zones());
        List<Simulation> saved = simulationService.saved();
        model.addAttribute("savedScenarios", saved);
        model.addAttribute("activeNav", "simulator");
        return "portal/simulator";
    }

    @PostMapping("/api/simulations/run")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> runPortalSimulation(
            @Valid @RequestBody SimulationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userDetails != null ? userService.findByEmail(userDetails.getUsername()).orElse(null) : null;
        Map<String, Object> result = simulationService.runPortal(request, user);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/portal/simulator/{id}/delete")
    public String deleteScenario(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        simulationService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Saved scenario deleted.");
        return "redirect:/portal/simulator";
    }
}
