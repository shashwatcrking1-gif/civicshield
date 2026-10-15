package com.civicshield.controller;

import com.civicshield.dto.ContactForm;
import com.civicshield.dto.SimulationRequest;
import com.civicshield.dto.SimulationResult;
import com.civicshield.service.CascadeRiskEngine;
import com.civicshield.service.CascadeRiskEngine.ZoneProfile;
import com.civicshield.service.SimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Public website pages (no login needed).
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final CascadeRiskEngine engine;
    private final SimulationService simulationService;

    @GetMapping("/")
    public String home(Model model) {
        // Pre-compute an impressive default cascade for the hero and interactive strip
        SimulationRequest heroReq = SimulationRequest.builder()
                .zone("Velachery")
                .rainfall(115.0)
                .traffic(82.0)
                .drainage(35.0)
                .build();
        SimulationResult heroCascade = simulationService.runPublic(heroReq);
        model.addAttribute("heroCascade", heroCascade);
        model.addAttribute("zones", engine.zones());
        return "index";
    }

    @GetMapping("/features")
    public String features() {
        return "features";
    }

    @GetMapping("/how-it-works")
    public String howItWorks(Model model) {
        SimulationRequest demoReq = SimulationRequest.builder()
                .zone("Velachery")
                .rainfall(115.0)
                .traffic(82.0)
                .drainage(35.0)
                .build();
        model.addAttribute("sampleCascade", simulationService.runPublic(demoReq));
        return "how-it-works";
    }

    @GetMapping("/demo")
    public String demo(Model model) {
        List<ZoneProfile> zones = engine.zones();
        model.addAttribute("zones", zones);
        SimulationRequest defaultReq = SimulationRequest.builder()
                .zone("Velachery")
                .rainfall(90.0)
                .traffic(75.0)
                .drainage(40.0)
                .build();
        model.addAttribute("initialCascade", simulationService.runPublic(defaultReq));
        return "demo";
    }

    @GetMapping("/technology")
    public String technology() {
        return "technology";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/team")
    public String team() {
        return "team";
    }

    @GetMapping("/contact")
    public String contact(@RequestParam(value = "success", required = false) String success, Model model) {
        if (!model.containsAttribute("contactForm")) {
            model.addAttribute("contactForm", new ContactForm());
        }
        model.addAttribute("success", success != null);
        return "contact";
    }
}
