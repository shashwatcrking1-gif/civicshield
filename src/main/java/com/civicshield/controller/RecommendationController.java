package com.civicshield.controller;

import com.civicshield.model.Recommendation;
import com.civicshield.model.RecommendationStatus;
import com.civicshield.service.RecommendationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * Controller for reviewing AI recommendations. Human authorities maintain final veto.
 */
@Controller
@RequestMapping("/portal/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public String recommendationsPage(@RequestParam(value = "status", required = false) RecommendationStatus status,
                                      Model model) {
        RecommendationStatus filter = (status != null) ? status : RecommendationStatus.PENDING;
        List<Recommendation> list = recommendationService.list(filter);
        Map<RecommendationStatus, Long> counts = recommendationService.counts();

        model.addAttribute("recommendations", list);
        model.addAttribute("counts", counts);
        model.addAttribute("currentStatus", filter);
        model.addAttribute("activeNav", "recommendations");
        return "portal/recommendations";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable("id") Long id,
                          @RequestParam(value = "redirectStatus", defaultValue = "PENDING") String redirectStatus,
                          RedirectAttributes redirectAttributes) {
        recommendationService.approve(id);
        redirectAttributes.addFlashAttribute("successMessage",
                "Action approved. Dispatched to corresponding municipal & emergency teams.");
        return "redirect:/portal/recommendations?status=" + redirectStatus;
    }

    @PostMapping("/{id}/dismiss")
    public String dismiss(@PathVariable("id") Long id,
                          @RequestParam(value = "redirectStatus", defaultValue = "PENDING") String redirectStatus,
                          RedirectAttributes redirectAttributes) {
        recommendationService.dismiss(id);
        redirectAttributes.addFlashAttribute("successMessage", "Recommendation dismissed with logged rationale.");
        return "redirect:/portal/recommendations?status=" + redirectStatus;
    }
}
