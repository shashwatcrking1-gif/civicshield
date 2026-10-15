package com.civicshield.controller;

import com.civicshield.dto.UserForm;
import com.civicshield.model.Role;
import com.civicshield.model.User;
import com.civicshield.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Admin-only controller for user accounts, role assignments, and enable/disable toggling.
 */
@Controller
@RequestMapping("/portal/admin/users")
public class UserAdminController {

    private final UserService userService;

    public UserAdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String listUsers(Model model) {
        List<User> users = userService.all();
        model.addAttribute("users", users);
        model.addAttribute("activeNav", "users");
        return "portal/admin/users";
    }

    @GetMapping("/new")
    public String newUserForm(Model model) {
        if (!model.containsAttribute("userForm")) {
            UserForm form = new UserForm();
            form.setRole(Role.AUTHORITY);
            model.addAttribute("userForm", form);
        }
        model.addAttribute("roles", Role.values());
        model.addAttribute("isNew", true);
        model.addAttribute("activeNav", "users");
        return "portal/admin/user-form";
    }

    @GetMapping("/{id}/edit")
    public String editUserForm(@PathVariable("id") Long id, Model model) {
        if (!model.containsAttribute("userForm")) {
            User user = userService.get(id);
            model.addAttribute("userForm", userService.toForm(user));
        }
        model.addAttribute("roles", Role.values());
        model.addAttribute("isNew", false);
        model.addAttribute("activeNav", "users");
        return "portal/admin/user-form";
    }

    @PostMapping("/save")
    public String saveUser(@Valid @ModelAttribute("userForm") UserForm form,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.userForm", bindingResult);
            redirectAttributes.addFlashAttribute("userForm", form);
            return form.isNew() ? "redirect:/portal/admin/users/new" : "redirect:/portal/admin/users/" + form.getUserId() + "/edit";
        }

        String error = userService.save(form);
        if (error != null) {
            redirectAttributes.addFlashAttribute("errorMessage", error);
            redirectAttributes.addFlashAttribute("userForm", form);
            return form.isNew() ? "redirect:/portal/admin/users/new" : "redirect:/portal/admin/users/" + form.getUserId() + "/edit";
        }

        redirectAttributes.addFlashAttribute("successMessage",
                "User account for '" + form.getName() + "' saved successfully.");
        return "redirect:/portal/admin/users";
    }

    @PostMapping("/{id}/toggle")
    public String toggleUser(@PathVariable("id") Long id,
                             @AuthenticationPrincipal UserDetails currentUser,
                             RedirectAttributes redirectAttributes) {
        try {
            User updated = userService.toggleEnabled(id, currentUser != null ? currentUser.getUsername() : "");
            String state = updated.isEnabled() ? "enabled" : "disabled";
            redirectAttributes.addFlashAttribute("successMessage",
                    "User account '" + updated.getName() + "' has been " + state + ".");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/portal/admin/users";
    }
}
