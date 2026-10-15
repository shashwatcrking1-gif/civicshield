package com.civicshield;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CivicShield – Civic Intelligence &amp; Crisis Vigilance Shield.
 * <p>
 * "Predict the chain. Break the chain." – an urban crisis prediction website that
 * forecasts cascading effects (Rain → Flood → Road Blockage → Traffic →
 * Ambulance Delay → Hospital Impact) and recommends action before they happen.
 */
@SpringBootApplication
public class CivicShieldApplication {

    public static void main(String[] args) {
        SpringApplication.run(CivicShieldApplication.class, args);
    }
}
