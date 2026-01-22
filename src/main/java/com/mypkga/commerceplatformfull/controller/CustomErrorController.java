package com.mypkga.commerceplatformfull.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@Slf4j
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object errorMessage = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object requestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            
            model.addAttribute("statusCode", statusCode);
            model.addAttribute("errorMessage", errorMessage);
            model.addAttribute("requestUri", requestUri);

            log.warn("Error occurred - Status: {}, URI: {}, Message: {}", 
                    statusCode, requestUri, errorMessage);

            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                model.addAttribute("title", "Page Not Found");
                model.addAttribute("description", "The page you are looking for might have been removed, had its name changed, or is temporarily unavailable.");
                return "error/404";
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                model.addAttribute("title", "Access Denied");
                model.addAttribute("description", "You don't have permission to access this resource.");
                return "error/403";
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                model.addAttribute("title", "Internal Server Error");
                model.addAttribute("description", "Something went wrong on our end. Please try again later.");
                return "error/500";
            }
        }

        model.addAttribute("title", "Error");
        model.addAttribute("description", "An unexpected error occurred.");
        return "error/generic";
    }
}