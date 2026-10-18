package com.civicshield.frontend.dto;

import java.time.LocalDateTime;

public record LoginResponse(String accessToken, String tokenType, LocalDateTime expiresAt, UserSummary user) {
    @Override
    public String toString() {
        return "LoginResponse[tokenType=" + tokenType + ", expiresAt=" + expiresAt + ", user=" + user + "]";
    }
}
