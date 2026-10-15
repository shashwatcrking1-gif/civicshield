package com.civicshield.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Public contact form. */
@Data
public class ContactForm {

    public ContactForm() {
    }

    @NotBlank(message = "Please tell us your name")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "Message cannot be empty")
    @Size(min = 10, max = 2000, message = "Message must be 10–2000 characters")
    private String message;
}
