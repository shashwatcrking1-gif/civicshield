package com.civicshield.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Role matrix
 * <pre>
 * ADMIN              everything (incl. /portal/admin/** and data sources)
 * AUTHORITY          incidents, risk, alerts, recommendations, simulator, reports
 * EMERGENCY_SERVICE  map, incidents (read-only), alerts, recommendations
 * HOSPITAL           read-only dashboard, map, alerts
 * </pre>
 * Every authenticated role can see the dashboard. CSRF stays enabled except for the stateless,
 * read-only public demo API.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ADMIN = "ADMIN", AUTHORITY = "AUTHORITY",
            EMERGENCY = "EMERGENCY_SERVICE", HOSPITAL = "HOSPITAL";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public website
                .requestMatchers("/", "/features", "/how-it-works", "/demo", "/technology", "/about",
                        "/team", "/contact", "/login", "/error", "/error/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/img/**", "/favicon.svg", "/favicon.ico", "/robots.txt").permitAll()
                .requestMatchers("/api/demo/**").permitAll()

                // Admin only
                .requestMatchers("/portal/admin/**", "/portal/data-sources/**").hasRole(ADMIN)

                // Authority tools
                .requestMatchers("/portal/simulator/**", "/api/simulations/**", "/portal/reports/**").hasAnyRole(ADMIN, AUTHORITY)
                .requestMatchers("/portal/risk/**", "/api/risk/**").hasAnyRole(ADMIN, AUTHORITY)

                // Map
                .requestMatchers("/portal/map/**").hasAnyRole(ADMIN, AUTHORITY, EMERGENCY, HOSPITAL)

                // Incidents: write = ADMIN/AUTHORITY, read also EMERGENCY_SERVICE
                .requestMatchers("/portal/incidents/new").hasAnyRole(ADMIN, AUTHORITY)
                .requestMatchers(HttpMethod.POST, "/portal/incidents/**").hasAnyRole(ADMIN, AUTHORITY)
                .requestMatchers("/portal/incidents/**").hasAnyRole(ADMIN, AUTHORITY, EMERGENCY)

                // Alerts: everyone reads, HOSPITAL cannot act
                .requestMatchers(HttpMethod.POST, "/portal/alerts/**").hasAnyRole(ADMIN, AUTHORITY, EMERGENCY)
                .requestMatchers("/portal/alerts/**").hasAnyRole(ADMIN, AUTHORITY, EMERGENCY, HOSPITAL)

                // Recommendations
                .requestMatchers("/portal/recommendations/**").hasAnyRole(ADMIN, AUTHORITY, EMERGENCY)

                // Dashboard + remaining portal/API
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
            .exceptionHandling(ex -> ex.accessDeniedPage("/error/403"))
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/demo/**"));
        return http.build();
    }
}
