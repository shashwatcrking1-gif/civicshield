package com.civicshield.service;

import com.civicshield.dto.SimulationRequest;
import com.civicshield.dto.SimulationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * Calls the optional Python AI micro-service ({@code POST /predict}). If the service is disabled,
 * slow, unreachable or returns an invalid payload, the built-in Java {@link CascadeRiskEngine}
 * answers instead – so the demo always works.
 */
@Slf4j
@Service
public class AiClientService {

    private final CascadeRiskEngine engine;
    private final WebClient webClient;
    private final boolean enabled;
    private final long timeoutMs;

    public AiClientService(CascadeRiskEngine engine,
                           WebClient.Builder builder,
                           @Value("${civicshield.ai.enabled:false}") boolean enabled,
                           @Value("${civicshield.ai.base-url:http://localhost:8000}") String baseUrl,
                           @Value("${civicshield.ai.timeout-ms:1500}") long timeoutMs) {
        this.engine = engine;
        this.webClient = builder.baseUrl(baseUrl).build();
        this.enabled = enabled;
        this.timeoutMs = timeoutMs;
    }

    public SimulationResult simulate(SimulationRequest request) {
        if (enabled) {
            try {
                SimulationResult remote = webClient.post()
                        .uri("/predict")
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(SimulationResult.class)
                        .timeout(Duration.ofMillis(timeoutMs))
                        .block();
                if (remote != null && remote.getRiskLevel() != null && !remote.getNodes().isEmpty()) {
                    remote.setEngine("PYTHON_AI");
                    return remote;
                }
                log.warn("AI service returned an incomplete payload – using Java engine");
            } catch (Exception ex) {
                log.warn("AI service unavailable ({}), falling back to Java CascadeRiskEngine", ex.getMessage());
            }
        }
        return engine.simulate(request);
    }

    public boolean isEnabled() {
        return enabled;
    }
}
