package com.civicshield.model;

import jakarta.persistence.*;
import lombok.*;

/** Portal user. Passwords are stored as BCrypt hashes. */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    /** Disabled users cannot sign in (managed by ADMIN). */
    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    public User() {
    }

    public User(Long userId, String name, String email, String password, Role role, boolean enabled) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.enabled = enabled;
    }

    /** Two-letter initials for avatars. */
    public String getInitials() {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].replaceAll("[^A-Za-z]", "");
        String last = parts.length > 1 ? parts[parts.length - 1].replaceAll("[^A-Za-z]", "") : "";
        String i = (first.isEmpty() ? "" : first.substring(0, 1)) + (last.isEmpty() ? "" : last.substring(0, 1));
        return i.isEmpty() ? "?" : i.toUpperCase();
    }
}
