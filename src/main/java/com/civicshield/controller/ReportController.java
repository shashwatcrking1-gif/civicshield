package com.civicshield.controller;

import com.civicshield.model.IncidentType;
import com.civicshield.model.RiskLevel;
import com.civicshield.service.ReportService;
import com.civicshield.service.ReportService.ReportRow;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for filtering incidents, analyzing distribution, and exporting CSV reports.
 */
@Controller
@RequestMapping("/portal/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public String reportsPage(@RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                              @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                              @RequestParam(value = "type", required = false) IncidentType type,
                              @RequestParam(value = "level", required = false) RiskLevel level,
                              @RequestParam(value = "q", required = false) String q,
                              Model model) {
        List<ReportRow> rows = reportService.rows(from, to, type, level, q);

        model.addAttribute("rows", rows);
        model.addAttribute("countByType", reportService.countByType(rows));
        model.addAttribute("countByLevel", reportService.countByLevel(rows));
        model.addAttribute("countByStatus", reportService.countByStatus(rows));
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedLevel", level);
        model.addAttribute("q", q);
        model.addAttribute("incidentTypes", IncidentType.values());
        model.addAttribute("riskLevels", RiskLevel.values());
        model.addAttribute("activeNav", "reports");
        return "portal/reports";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(@RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                            @RequestParam(value = "type", required = false) IncidentType type,
                                            @RequestParam(value = "level", required = false) RiskLevel level,
                                            @RequestParam(value = "q", required = false) String q) {
        List<ReportRow> rows = reportService.rows(from, to, type, level, q);
        String csv = reportService.toCsv(rows);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        String filename = "civicshield-report-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }
}
