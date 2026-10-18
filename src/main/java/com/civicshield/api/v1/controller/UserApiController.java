package com.civicshield.api.v1.controller;

import com.civicshield.api.v1.auth.TokenService;
import com.civicshield.api.v1.dto.CommonDtos.UserRequest;
import com.civicshield.api.v1.dto.CommonDtos.UserSummary;
import com.civicshield.exception.ApiException;
import com.civicshield.model.Role;
import com.civicshield.model.User;
import com.civicshield.repository.UserRepository;
import com.civicshield.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
public class UserApiController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserApiController(UserService userService, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public ResponseEntity<List<UserSummary>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean enabled) {

        List<UserSummary> list = userService.all().stream()
                .filter(u -> role == null || u.getRole() == role)
                .filter(u -> enabled == null || u.isEnabled() == enabled)
                .filter(u -> {
                    if (q == null || q.isBlank()) return true;
                    String term = q.trim().toLowerCase(Locale.ROOT);
                    return u.getName().toLowerCase(Locale.ROOT).contains(term)
                            || u.getEmail().toLowerCase(Locale.ROOT).contains(term);
                })
                .map(UserSummary::from)
                .toList();

        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<UserSummary> create(@Valid @RequestBody UserRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        if (req.password() == null || req.password().length() < 8 || req.password().length() > 72) {
            throw ApiException.badRequest("Password must be between 8 and 72 characters");
        }

        Role role = parseRole(req.role());
        boolean enabled = req.enabled() == null || req.enabled();

        User user = userRepository.save(User.builder()
                .name(req.name().trim())
                .email(email)
                .password(passwordEncoder.encode(req.password()))
                .role(role)
                .enabled(enabled)
                .build());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(user.getUserId()).toUri();
        return ResponseEntity.created(location).body(UserSummary.from(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserSummary> update(@PathVariable Long id, @Valid @RequestBody UserRequest req) {
        User user = userService.get(id);
        String email = req.email().trim().toLowerCase(Locale.ROOT);

        Optional<User> existing = userRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent() && !existing.get().getUserId().equals(id)) {
            throw ApiException.conflict("An account with this email already exists");
        }

        if (req.password() != null && !req.password().isBlank()) {
            if (req.password().length() < 8 || req.password().length() > 72) {
                throw ApiException.badRequest("Password must be between 8 and 72 characters");
            }
            user.setPassword(passwordEncoder.encode(req.password()));
        }

        user.setName(req.name().trim());
        user.setEmail(email);
        user.setRole(parseRole(req.role()));
        if (req.enabled() != null) {
            user.setEnabled(req.enabled());
        }

        userRepository.save(user);
        return ResponseEntity.ok(UserSummary.from(user));
    }

    @PostMapping("/{id}/toggle-enabled")
    public ResponseEntity<UserSummary> toggleEnabled(@PathVariable Long id, Authentication authentication) {
        String currentEmail = resolveEmail(authentication);
        User user = userService.get(id);
        if (user.getEmail().equalsIgnoreCase(currentEmail)) {
            throw ApiException.conflict("You cannot disable your own account");
        }

        User updated = userService.toggleEnabled(id, currentEmail);
        return ResponseEntity.ok(UserSummary.from(updated));
    }

    private Role parseRole(String roleStr) {
        try {
            return Role.valueOf(roleStr.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw ApiException.badRequest("Invalid role: " + roleStr);
        }
    }

    private String resolveEmail(Authentication authentication) {
        if (authentication == null) return "";
        if (authentication.getPrincipal() instanceof TokenService.TokenInfo info) {
            return info.email();
        }
        return authentication.getName();
    }
}
