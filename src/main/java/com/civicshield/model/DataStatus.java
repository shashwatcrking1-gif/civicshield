package com.civicshield.model;

import lombok.Getter;

/** Health of an external data feed. */
@Getter
public enum DataStatus {
    ONLINE("Online", "ok"),
    DELAYED("Delayed", "warn"),
    OFFLINE("Offline", "danger");

    private final String label;
    private final String css;

    DataStatus(String label, String css) {
        this.label = label;
        this.css = css;
    }
}
