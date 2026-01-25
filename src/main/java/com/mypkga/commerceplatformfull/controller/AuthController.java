package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new User());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("user") User user,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        // Validation - only username and password required
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            model.addAttribute("error", "Username is required");
            return "auth/register";
        }

        if (user.getPassword() == null || user.getPassword().length() < 6) {
            model.addAttribute("error", "Password must be at least 6 characters");
            return "auth/register";
        }

        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            model.addAttribute("error", "Full name is required");
            return "auth/register";
        }

        // Check if username already exists
        if (userService.existsByUsername(user.getUsername())) {
            model.addAttribute("error", "Username already exists");
            return "auth/register";
        }

        try {
            userService.registerUser(user);
            log.info("User registered successfully: {} with role: {}", 
                    user.getUsername(), user.getRoleName());
            redirectAttributes.addFlashAttribute("success", 
                    "Registration successful! Please login with your username and password.");
            return "redirect:/login";
        } catch (Exception e) {
            log.error("Registration failed for user: {}", user.getUsername(), e);
            model.addAttribute("error", "Registration failed. Please try again.");
            return "auth/register";
        }
    }
}
