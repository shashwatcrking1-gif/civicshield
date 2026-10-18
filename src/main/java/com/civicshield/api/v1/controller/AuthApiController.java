package com.civicshield.api.v1.controller;

import com.civicshield.api.v1.auth.TokenService;
import com.civicshield.api.v1.dto.CommonDtos.LoginRequest;
import com.civicshield.api.v1.dto.CommonDtos.LoginResponse;
import com.civicshield.api.v1.dto.CommonDtos.UserSummary;
import com.civicshield.exception.ApiException;
import com.civicshield.model.User;
import com.civicshield.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthApiController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthApiController(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String email = request.email().trim();
        if (tokenService.isRateLimited(email)) {
            throw ApiException.tooManyRequests("Too many failed login attempts. Please try again in 5 minutes.", 300);
        }

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || !user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPassword())) {
            tokenService.recordFailedLogin(email);
            throw ApiException.invalidCredentials("Invalid email or password");
        }

        tokenService.resetFailedAttempts(email);
        TokenService.TokenResult token = tokenService.createToken(user);
        return ResponseEntity.ok(new LoginResponse(
                token.rawToken(),
                "Bearer",
                token.expiresAt(),
                UserSummary.from(user)
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            tokenService.revokeToken(auth.substring(7).trim());
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserSummary> me(Authentication authentication) {
        if (authentication == null) throw ApiException.unauthorized("Authentication required");
        String email = authentication.getName();
        if (authentication.getPrincipal() instanceof TokenService.TokenInfo info) {
            email = info.email();
        }
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.unauthorized("User not found"));
        return ResponseEntity.ok(UserSummary.from(user));
    }
}
