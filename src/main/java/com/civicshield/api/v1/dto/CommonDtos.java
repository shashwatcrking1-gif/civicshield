package com.civicshield.api.v1.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.Map;

public final class CommonDtos {
    private CommonDtos() {}

    public record UserSummary(Long id, String name, String email, String role, String roleLabel, boolean enabled, String initials) {
        public static UserSummary from(com.civicshield.model.User u) {
            if (u == null) return null;
            String initials = u.getName() != null && !u.getName().isBlank()
                    ? u.getName().chars().filter(Character::isUpperCase).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString()
                    : "CS";
            if (initials.isBlank()) initials = u.getName() != null && !u.getName().isBlank() ? u.getName().substring(0, 1).toUpperCase() : "CS";
            return new UserSummary(u.getUserId(), u.getName(), u.getEmail(), u.getRole().name(), u.getRole().getLabel(), u.isEnabled(), initials);
        }
    }

    public record LoginRequest(@NotBlank(message = "Email is required") String email, @NotBlank(message = "Password is required") String password) {}

    public record LoginResponse(String accessToken, String tokenType, LocalDateTime expiresAt, UserSummary user) {}

    public record UserRequest(@NotBlank(message = "Name is required") String name,
                              @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,
                              String password,
                              @NotBlank(message = "Role is required") String role,
                              Boolean enabled) {}

    public record SystemStatus(String status, String application, String version, String apiVersion,
                               LocalDateTime serverTime, String timezone, String dataProvenance,
                               String riskEngine, boolean aiServiceEnabled, Map<String, Long> dataSources) {}

    public record DataSourceDto(Long id, String name, String type, String location, String status) {
        public static DataSourceDto from(com.civicshield.model.DataSource ds) {
            return new DataSourceDto(ds.getSourceId(), ds.getSourceName(), ds.getSourceType(), ds.getLocation(), ds.getDataStatus().name());
        }
    }
}
