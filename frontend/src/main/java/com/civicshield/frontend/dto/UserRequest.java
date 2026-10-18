package com.civicshield.frontend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Create/update body for {@code /users}. {@code password} is optional on update and never logged. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserRequest(String name, String email, String password, Role role, boolean enabled) {
    @Override
    public String toString() {
        return "UserRequest[name=" + name + ", email=" + email + ", role=" + role + ", enabled=" + enabled + "]";
    }
}
