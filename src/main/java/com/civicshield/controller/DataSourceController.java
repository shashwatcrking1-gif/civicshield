package com.civicshield.controller;

import com.civicshield.model.DataSource;
import com.civicshield.model.DataStatus;
import com.civicshield.service.DataSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * Controller for viewing external data sources status and simulating status changes.
 */
@Controller
@RequestMapping("/portal/data-sources")
@RequiredArgsConstructor
public class DataSourceController {

    private final DataSourceService dataSourceService;

    @GetMapping
    public String dataSourcesPage(Model model) {
        List<DataSource> sources = dataSourceService.all();
        Map<DataStatus, Long> counts = dataSourceService.counts();

        model.addAttribute("sources", sources);
        model.addAttribute("counts", counts);
        model.addAttribute("statuses", DataStatus.values());
        model.addAttribute("activeNav", "data-sources");
        return "portal/data-sources";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable("id") Long id,
                               @RequestParam("status") DataStatus status,
                               RedirectAttributes redirectAttributes) {
        DataSource updated = dataSourceService.updateStatus(id, status);
        redirectAttributes.addFlashAttribute("successMessage",
                "Data source '" + updated.getSourceName() + "' updated to " + status.getLabel() + ".");
        return "redirect:/portal/data-sources";
    }
}
