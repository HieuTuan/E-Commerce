package com.mypkga.commerceplatformfull.security;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.regex.Pattern;

@Aspect
@Component
@Slf4j
public class InputValidationAspect {

    // Common SQL injection patterns
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i).*(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|vbscript|onload|onerror).*"
    );

    // XSS patterns
    private static final Pattern XSS_PATTERN = Pattern.compile(
            "(?i).*(<script|</script|javascript:|vbscript:|onload=|onerror=|onclick=|onmouseover=).*"
    );

    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping)")
    public Object validateInput(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof String) {
                String input = (String) args[i];
                args[i] = sanitizeInput(input);
            }
        }
        
        return joinPoint.proceed(args);
    }

    private String sanitizeInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        // Check for SQL injection attempts
        if (SQL_INJECTION_PATTERN.matcher(input).matches()) {
            log.warn("Potential SQL injection attempt detected: {}", input);
            throw new SecurityException("Invalid input detected");
        }

        // Check for XSS attempts
        if (XSS_PATTERN.matcher(input).matches()) {
            log.warn("Potential XSS attempt detected: {}", input);
            throw new SecurityException("Invalid input detected");
        }

        // HTML escape the input
        return HtmlUtils.htmlEscape(input);
    }
}