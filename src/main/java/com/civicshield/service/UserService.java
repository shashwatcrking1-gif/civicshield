package com.civicshield.service;

import com.civicshield.dto.UserForm;
import com.civicshield.exception.ResourceNotFoundException;
import com.civicshield.model.User;
import com.civicshield.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** User management and Spring Security user lookup (email is the username). */
@Service
public class UserService implements UserDetailsService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User u = repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + email));
        return org.springframework.security.core.userdetails.User.withUsername(u.getEmail())
                .password(u.getPassword())
                .roles(u.getRole().name())
                .disabled(!u.isEnabled())
                .build();
    }

    public Optional<User> findByEmail(String email) {
        return email == null ? Optional.empty() : repository.findByEmailIgnoreCase(email);
    }

    public User get(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    public List<User> all() {
        return repository.findAll(Sort.by("role", "name"));
    }

    public UserForm toForm(User u) {
        UserForm f = new UserForm();
        f.setUserId(u.getUserId());
        f.setName(u.getName());
        f.setEmail(u.getEmail());
        f.setRole(u.getRole());
        f.setEnabled(u.isEnabled());
        return f;
    }

    /**
     * Creates or updates a user. Returns an error message key-less string when a business rule fails
     * (duplicate email, missing/short password), otherwise {@code null}.
     */
    @Transactional
    public String save(UserForm form) {
        String email = form.getEmail().trim().toLowerCase();
        String pwd = form.getPassword() == null ? "" : form.getPassword().trim();
        if (form.isNew()) {
            if (repository.existsByEmailIgnoreCase(email)) return "An account with this email already exists";
            if (pwd.length() < 8) return "Password must be at least 8 characters";
            repository.save(User.builder().name(form.getName().trim()).email(email)
                    .password(passwordEncoder.encode(pwd)).role(form.getRole()).enabled(form.isEnabled()).build());
            return null;
        }
        User u = get(form.getUserId());
        Optional<User> other = repository.findByEmailIgnoreCase(email);
        if (other.isPresent() && !other.get().getUserId().equals(u.getUserId()))
            return "An account with this email already exists";
        if (!pwd.isEmpty() && pwd.length() < 8) return "Password must be at least 8 characters";
        u.setName(form.getName().trim());
        u.setEmail(email);
        u.setRole(form.getRole());
        u.setEnabled(form.isEnabled());
        if (!pwd.isEmpty()) u.setPassword(passwordEncoder.encode(pwd));
        return null;
    }

    @Transactional
    public User toggleEnabled(Long id, String currentEmail) {
        User u = get(id);
        if (u.getEmail().equalsIgnoreCase(currentEmail)) {
            throw new IllegalStateException("You cannot disable your own account");
        }
        u.setEnabled(!u.isEnabled());
        return u;
    }

    @Transactional
    public User create(String name, String email, String rawPassword, com.civicshield.model.Role role) {
        return repository.save(User.builder().name(name).email(email.toLowerCase())
                .password(passwordEncoder.encode(rawPassword)).role(role).enabled(true).build());
    }

    public long count() {
        return repository.count();
    }
}
