package com.civicshield.frontend.dto;

/** Credentials are only held for the duration of the login call and never stored. */
public record LoginRequest(String email, String password) {
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=***]";
    }
}
