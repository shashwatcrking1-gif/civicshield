package com.civicshield.frontend.dto;

/** Engine-recommended mitigation ({@code Action} in docs/API.md). */
public record MitigationAction(String text, Level priority, String target) {
}
