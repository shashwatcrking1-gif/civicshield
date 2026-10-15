package com.civicshield.dto;

import com.civicshield.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/** ADMIN form to create or edit a portal user. Password is optional when editing. */
@Data
public class UserForm {

    public UserForm() {
    }

    private Long userId;

    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 150)
    private String email;

    /** Required for new users (checked in the service), min 8 characters when provided. */
    @Size(max = 72, message = "Password must be at most 72 characters")
    private String password;

    @NotNull(message = "Select a role")
    private Role role;

    private boolean enabled = true;

    public boolean isNew() {
        return userId == null;
    }
}
