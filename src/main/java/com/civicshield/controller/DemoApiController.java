package com.civicshield.controller;

import com.civicshield.dto.SimulationRequest;
import com.civicshield.dto.SimulationResult;
import com.civicshield.service.SimulationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public REST endpoint for the interactive What-If Simulator widget and demo page.
 */
@RestController
@RequestMapping("/api/demo")
public class DemoApiController {

    private final SimulationService simulationService;

    public DemoApiController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping("/simulate")
    public ResponseEntity<SimulationResult> simulate(@Valid @RequestBody SimulationRequest request) {
        SimulationResult result = simulationService.runPublic(request);
        return ResponseEntity.ok(result);
    }
}
