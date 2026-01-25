package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.service.OTPService;
import com.mypkga.commerceplatformfull.service.UserService;
import com.mypkga.commerceplatformfull.service.EmailService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final OTPService otpService;
    private final EmailService emailService;

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
            @RequestParam("confirmPassword") String confirmPassword,
            @RequestParam(value = "birthDate", required = false) String birthDate,
            BindingResult result,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        // Use email as username
        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            user.setUsername(user.getEmail());
        }
        
        // Full Name validation
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            model.addAttribute("error", "Full name is required");
            return "auth/register";
        }
        
        if (user.getFullName().length() > 100) {
            model.addAttribute("error", "Full name must not exceed 100 characters");
            return "auth/register";
        }
        
        // Check if each word starts with uppercase letter
        String[] words = user.getFullName().trim().split("\\s+");
        for (String word : words) {
            if (word.length() > 0 && !Character.isUpperCase(word.charAt(0))) {
                model.addAttribute("error", "Each word in full name must start with an uppercase letter");
                return "auth/register";
            }
        }

        // Date of Birth validation
        if (birthDate != null && !birthDate.trim().isEmpty()) {
            try {
                LocalDate birth = LocalDate.parse(birthDate);
                LocalDate today = LocalDate.now();
                
                if (birth.isAfter(today)) {
                    model.addAttribute("error", "Date of birth cannot be in the future");
                    return "auth/register";
                }
                
                int age = Period.between(birth, today).getYears();
                if (age < 15) {
                    model.addAttribute("error", "You must be at least 15 years old");
                    return "auth/register";
                }
            } catch (Exception e) {
                model.addAttribute("error", "Invalid date of birth format");
                return "auth/register";
            }
        } else {
            model.addAttribute("error", "Date of birth is required");
            return "auth/register";
        }

        // Phone number validation
        if (user.getPhone() == null || user.getPhone().trim().isEmpty()) {
            model.addAttribute("error", "Phone number is required");
            return "auth/register";
        }
        
        if (!user.getPhone().matches("^(03|05|07|08|09)[0-9]{8}$")) {
            model.addAttribute("error", "Phone number must be 10 digits starting with 03, 05, 07, 08, or 09");
            return "auth/register";
        }

        // Email validation
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            model.addAttribute("error", "Email is required");
            return "auth/register";
        }

        if (!emailService.isValidEmail(user.getEmail())) {
            model.addAttribute("error", "Invalid email format");
            return "auth/register";
        }

        // Password validation
        if (user.getPassword() == null || user.getPassword().length() < 6) {
            model.addAttribute("error", "Password must be at least 6 characters");
            return "auth/register";
        }
        
        if (!user.getPassword().matches(".*[a-z].*")) {
            model.addAttribute("error", "Password must contain at least one lowercase letter");
            return "auth/register";
        }
        
        if (!user.getPassword().matches(".*[A-Z].*")) {
            model.addAttribute("error", "Password must contain at least one uppercase letter");
            return "auth/register";
        }
        
        if (!user.getPassword().matches(".*\\d.*")) {
            model.addAttribute("error", "Password must contain at least one number");
            return "auth/register";
        }
        
        if (!user.getPassword().matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            model.addAttribute("error", "Password must contain at least one special character");
            return "auth/register";
        }

        // Validate password confirmation
        if (!user.getPassword().equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match");
            return "auth/register";
        }

        // Check if email already exists
        if (userService.existsByEmail(user.getEmail())) {
            model.addAttribute("error", "Email already exists");
            return "auth/register";
        }

        // Store user data in session for verification
        session.setAttribute("pendingUser", user);
        
        // Send OTP for email verification
        try {
            boolean otpSent = otpService.generateAndSendOTP(user.getEmail());
            if (!otpSent) {
                model.addAttribute("error", "Failed to send verification code. Please try again.");
                return "auth/register";
            }
            
            // Redirect to email verification page
            model.addAttribute("email", maskEmail(user.getEmail()));
            model.addAttribute("fullEmail", user.getEmail());
            return "auth/verify-email";
            
        } catch (IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        } catch (Exception e) {
            log.error("Failed to send OTP for registration: {}", user.getEmail(), e);
            model.addAttribute("error", "Failed to send verification code. Please try again.");
            return "auth/register";
        }
    }

    @GetMapping("/verify-email")
    public String verifyEmailPage(Model model, HttpSession session) {
        User pendingUser = (User) session.getAttribute("pendingUser");
        if (pendingUser == null) {
            return "redirect:/register";
        }
        
        model.addAttribute("email", maskEmail(pendingUser.getEmail()));
        model.addAttribute("fullEmail", pendingUser.getEmail());
        return "auth/verify-email";
    }

    @PostMapping("/verify-email")
    public String verifyEmail(@RequestParam("otp") String otp,
                             HttpSession session,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        
        User pendingUser = (User) session.getAttribute("pendingUser");
        if (pendingUser == null) {
            return "redirect:/register";
        }

        if (otp == null || otp.trim().isEmpty()) {
            model.addAttribute("error", "Verification code is required");
            model.addAttribute("email", maskEmail(pendingUser.getEmail()));
            model.addAttribute("fullEmail", pendingUser.getEmail());
            return "auth/verify-email";
        }

        try {
            boolean isValid = otpService.validateOTP(pendingUser.getEmail(), otp);
            if (isValid) {
                // Mark email as verified and complete registration
                pendingUser.setEmailVerified(true);
                userService.registerUser(pendingUser);
                
                // Clear session
                session.removeAttribute("pendingUser");
                
                log.info("User registered successfully with verified email: {}", pendingUser.getUsername());
                redirectAttributes.addFlashAttribute("success", 
                        "Registration successful! Your email has been verified. Please login.");
                return "redirect:/login";
            } else {
                int remainingAttempts = otpService.getRemainingAttempts(pendingUser.getEmail());
                model.addAttribute("error", 
                    String.format("Invalid verification code. %d attempts remaining.", remainingAttempts));
                model.addAttribute("email", maskEmail(pendingUser.getEmail()));
                model.addAttribute("fullEmail", pendingUser.getEmail());
                return "auth/verify-email";
            }
        } catch (Exception e) {
            log.error("Email verification failed for user: {}", pendingUser.getUsername(), e);
            model.addAttribute("error", "Verification failed. Please try again.");
            model.addAttribute("email", maskEmail(pendingUser.getEmail()));
            model.addAttribute("fullEmail", pendingUser.getEmail());
            return "auth/verify-email";
        }
    }

    @PostMapping("/api/resend-otp")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> resendOTP(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        User pendingUser = (User) session.getAttribute("pendingUser");
        if (pendingUser == null) {
            response.put("success", false);
            response.put("message", "Session expired. Please start registration again.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            boolean otpSent = otpService.resendOTP(pendingUser.getEmail());
            if (otpSent) {
                response.put("success", true);
                response.put("message", "Verification code sent successfully");
            } else {
                long secondsUntilNext = otpService.getSecondsUntilNextRequest(pendingUser.getEmail());
                response.put("success", false);
                response.put("message", "Please wait " + secondsUntilNext + " seconds before requesting again");
                response.put("waitTime", secondsUntilNext);
            }
        } catch (IllegalStateException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to resend OTP", e);
            response.put("success", false);
            response.put("message", "Failed to send verification code. Please try again.");
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/otp-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOTPStatus(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        User pendingUser = (User) session.getAttribute("pendingUser");
        if (pendingUser == null) {
            response.put("success", false);
            response.put("message", "Session expired");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // Use email for OTP status
            String email = pendingUser.getEmail();
            
            int remainingAttempts = otpService.getRemainingAttempts(email);
            long secondsUntilNext = otpService.getSecondsUntilNextRequest(email);
            boolean isBlocked = otpService.isBlocked(email);
            
            response.put("success", true);
            response.put("remainingAttempts", remainingAttempts);
            response.put("secondsUntilNextRequest", secondsUntilNext);
            response.put("isBlocked", isBlocked);
        } catch (Exception e) {
            log.error("Failed to get OTP status", e);
            response.put("success", false);
            response.put("message", "Failed to get status");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Mask email for display (show only first 2 chars and domain)
     */
    private String maskEmail(String email) {
        if (email == null || email.length() < 3) {
            return "***";
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "*" + domain;
        } else {
            return localPart.substring(0, 2) + "***" + domain;
        }
    }

    /**
     * Validate Vietnamese phone number format (10 digits starting with 03, 05, 07, 08, 09)
     */
    private boolean isValidPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        
        String cleanPhone = phone.replaceAll("\\s+", "");
        // Updated Vietnamese phone number pattern: 10 digits starting with 03, 05, 07, 08, 09
        return cleanPhone.matches("^(03|05|07|08|09)[0-9]{8}$");
    }

    /**
     * Mask phone number for display (show only first 3 and last 2 digits)
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) {
            return "***";
        }
        
        String cleanPhone = phone.replaceAll("\\s+", "");
        if (cleanPhone.length() < 6) {
            return "***";
        }
        
        return cleanPhone.substring(0, 3) + "***" + cleanPhone.substring(cleanPhone.length() - 2);
    }
}
