package com.civicshield.controller;

import com.civicshield.dto.DashboardStats;
import com.civicshield.model.Alert;
import com.civicshield.model.Incident;
import com.civicshield.service.AlertService;
import com.civicshield.service.IncidentService;
import com.civicshield.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * Authority portal dashboard and associated JSON endpoints for live stats and charts.
 */
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final ReportService reportService;
    private final AlertService alertService;
    private final IncidentService incidentService;

    @GetMapping({"/portal", "/portal/dashboard"})
    public String dashboard(Model model) {
        DashboardStats stats = reportService.dashboardStats();
        List<Alert> recentAlerts = alertService.recentOpen();
        List<Incident> openIncidents = incidentService.open();

        model.addAttribute("stats", stats);
        model.addAttribute("recentAlerts", recentAlerts);
        model.addAttribute("openIncidents", openIncidents);
        model.addAttribute("activeNav", "dashboard");
        return "portal/dashboard";
    }

    @GetMapping("/api/dashboard/stats")
    @ResponseBody
    public ResponseEntity<DashboardStats> getStats() {
        return ResponseEntity.ok(reportService.dashboardStats());
    }

    @GetMapping("/api/dashboard/trend")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTrend(@RequestParam(defaultValue = "14") int days) {
        return ResponseEntity.ok(reportService.trend(days));
    }
}
