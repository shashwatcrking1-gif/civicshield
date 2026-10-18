package com.civicshield.config;

import com.civicshield.api.v1.auth.BearerAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.time.LocalDateTime;

/**
 * Role matrix
 * <pre>
 * ADMIN              everything (incl. /portal/admin/**, /api/v1/users/**, data sources)
 * AUTHORITY          incidents, risk, alerts, recommendations, simulator, reports
 * EMERGENCY_SERVICE  map, incidents (read-only), alerts, recommendations
 * HOSPITAL           read-only dashboard, map, alerts
 * </pre>
 * Every authenticated role can see the dashboard. CSRF stays enabled except for the stateless REST API (/api/**).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ADMIN = "ADMIN", AUTHORITY = "AUTHORITY",
            EMERGENCY = "EMERGENCY_SERVICE", HOSPITAL = "HOSPITAL";

    private final BearerAuthFilter bearerAuthFilter;

    public SecurityConfig(BearerAuthFilter bearerAuthFilter) {
        this.bearerAuthFilter = bearerAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .addFilterBefore(bearerAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // Public website & public APIs
                .requestMatchers("/", "/features", "/how-it-works", "/demo", "/technology", "/about",
                        "/team", "/contact", "/login", "/error", "/error/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/img/**", "/favicon.svg", "/favicon.ico", "/robots.txt").permitAll()
                .requestMatchers("/api/demo/**").permitAll()
                .requestMatchers("/api/v1/auth/login", "/api/v1/system/status").permitAll()

                // Admin only
                .requestMatchers("/portal/admin/**", "/portal/data-sources/**").hasRole(ADMIN)
                .requestMatchers("/api/v1/users/**").hasRole(ADMIN)

                // Authority tools
                .requestMatchers("/portal/simulator/**", "/api/simulations/**", "/portal/reports/**").hasAnyRole(ADMIN, AUTHORITY)
                .requestMatchers("/portal/risk/**", "/api/risk/**").hasAnyRole(ADMIN, AUTHORITY)
                .requestMatchers("/api/v1/simulations/**", "/api/v1/reports/**").hasAnyRole(ADMIN, AUTHORITY)
                .requestMatchers(HttpMethod.POST, "/api/v1/incidents/*/cascade").hasAnyRole(ADMIN, AUTHORITY)

                // Map & read-only shared resources
                .requestMatchers("/portal/map/**").hasAnyRole(ADMIN, AUTHORITY, EMERGENCY, HOSPITAL)
                .requestMatchers("/api/v1/map/**", "/api/v1/zones", "/api/v1/data-sources").hasAnyRole(ADMIN, AUTHORITY, EMERGENCY, HOSPITAL)
                .requestMatchers("/api/v1/risk/model").hasAnyRole(ADMIN, AUTHORITY, EMERGENCY, HOSPITAL)

                // Incidents
                .requestMatchers("/portal/incidents/new").hasAnyRole(ADMIN, AUTHORITY)
                .requestMatchers(HttpMethod.POST, "/portal/incidents/**").hasAnyRole(ADMIN, AUTHORITY)
                .requestMatchers("/portal/incidents/**").hasAnyRole(ADMIN, AUTHORITY, EMERGENCY)
                .requestMatchers(HttpMethod.POST, "/api/v1/incidents").hasAnyRole(ADMIN, AUTHORITY)
                .requestMatchers(HttpMethod.PUT, "/api/v1/incidents/**").hasAnyRole(ADMIN, AUTHORITY)
                .requestMatchers(HttpMethod.PATCH, "/api/v1/incidents/**").hasAnyRole(ADMIN, AUTHORITY)
                .requestMatchers("/api/v1/incidents/**").hasAnyRole(ADMIN, AUTHORITY, EMERGENCY)

                // Alerts: everyone reads, HOSPITAL cannot act
                .requestMatchers(HttpMethod.POST, "/portal/alerts/**").hasAnyRole(ADMIN, AUTHORITY, EMERGENCY)
                .requestMatchers("/portal/alerts/**").hasAnyRole(ADMIN, AUTHORITY, EMERGENCY, HOSPITAL)
                .requestMatchers(HttpMethod.POST, "/api/v1/alerts/**").hasAnyRole(ADMIN, AUTHORITY, EMERGENCY)
                .requestMatchers("/api/v1/alerts/**").hasAnyRole(ADMIN, AUTHORITY, EMERGENCY, HOSPITAL)

                // Recommendations
                .requestMatchers("/portal/recommendations/**").hasAnyRole(ADMIN, AUTHORITY, EMERGENCY)
                .requestMatchers("/api/v1/recommendations/**").hasAnyRole(ADMIN, AUTHORITY, EMERGENCY)

                // Dashboard + remaining portal/API
                .requestMatchers("/api/v1/dashboard/**", "/api/v1/auth/me", "/api/v1/auth/logout").hasAnyRole(ADMIN, AUTHORITY, EMERGENCY, HOSPITAL)
                .requestMatchers("/portal/**", "/api/**").authenticated()
                .anyRequest().permitAll())
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .defaultSuccessUrl("/portal/dashboard", true)
                .failureUrl("/login?error")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll())
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write(String.format("{\"timestamp\":\"%s\",\"status\":401,\"error\":\"unauthorized\",\"message\":\"Authentication required\",\"path\":\"%s\"}",
                            LocalDateTime.now(), request.getRequestURI()));
                    response.getWriter().flush();
                }, new AntPathRequestMatcher("/api/**"))
                .defaultAccessDeniedHandlerFor((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write(String.format("{\"timestamp\":\"%s\",\"status\":403,\"error\":\"forbidden\",\"message\":\"Access denied\",\"path\":\"%s\"}",
                            LocalDateTime.now(), request.getRequestURI()));
                    response.getWriter().flush();
                }, new AntPathRequestMatcher("/api/**"))
                .defaultAccessDeniedHandlerFor((request, response, accessDeniedException) -> {
                    request.getRequestDispatcher("/error/403").forward(request, response);
                }, new AntPathRequestMatcher("/**")))
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));

        return http.build();
    }
}
