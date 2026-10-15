package com.civicshield.controller;

import com.civicshield.model.Alert;
import com.civicshield.model.AlertStatus;
import com.civicshield.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * Portal alerts view and state transition actions (Acknowledge, Resolve).
 */
@Controller
@RequestMapping("/portal/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public String alertsPage(@RequestParam(value = "status", defaultValue = "ACTIVE") AlertStatus status,
                             Model model) {
        List<Alert> alerts = alertService.byStatus(status);
        Map<AlertStatus, Long> counts = alertService.counts();

        model.addAttribute("alerts", alerts);
        model.addAttribute("counts", counts);
        model.addAttribute("currentStatus", status);
        model.addAttribute("activeNav", "alerts");
        return "portal/alerts";
    }

    @PostMapping("/{id}/acknowledge")
    public String acknowledgeAlert(@PathVariable("id") Long id,
                                   @RequestParam(value = "redirectStatus", defaultValue = "ACTIVE") String redirectStatus,
                                   RedirectAttributes redirectAttributes) {
        alertService.acknowledge(id);
        redirectAttributes.addFlashAttribute("successMessage", "Alert acknowledged by authority.");
        return "redirect:/portal/alerts?status=" + redirectStatus;
    }

    @PostMapping("/{id}/resolve")
    public String resolveAlert(@PathVariable("id") Long id,
                               @RequestParam(value = "redirectStatus", defaultValue = "ACTIVE") String redirectStatus,
                               RedirectAttributes redirectAttributes) {
        alertService.resolve(id);
        redirectAttributes.addFlashAttribute("successMessage", "Alert marked as resolved.");
        return "redirect:/portal/alerts?status=" + redirectStatus;
    }
}
