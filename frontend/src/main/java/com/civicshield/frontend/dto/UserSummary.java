package com.civicshield.frontend.dto;

public record UserSummary(Long id, String name, String email, Role role, String roleLabel, boolean enabled,
                          String initials) {
    public String displayRole() {
        return roleLabel != null ? roleLabel : role != null ? role.label() : "Unknown role";
    }
}
