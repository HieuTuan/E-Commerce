package com.mypkga.commerceplatformfull.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller để redirect user đến trang đúng dựa trên role
 */
@Controller
public class RedirectController {
    
    @GetMapping("/redirect-by-role")
    public String redirectByRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated()) {
            String role = auth.getAuthorities().iterator().next().getAuthority();
            
            switch (role) {
                case "ROLE_POST_OFFICE":
                    return "redirect:/postoffice";
                case "ROLE_STAFF":
                    return "redirect:/staff/returns";
                case "ROLE_ADMIN":
                    return "redirect:/admin";
                default:
                    return "redirect:/";
            }
        }
        
        return "redirect:/login";
    }
}