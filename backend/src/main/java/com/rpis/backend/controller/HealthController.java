package com.rpis.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Autowired;
import com.rpis.backend.service.TokenService;

@RestController
@RequestMapping("/api")
public class HealthController {

    @Autowired
    private TokenService tokenService;

    @GetMapping("/health")
    public String healthCheck() {
        return "RPIS backend is running";
    }

    @GetMapping("/test-token")
    public String testToken(@RequestParam String token) {
        boolean valid = tokenService.validateToken(token);
        if (!valid) {
            return "Token is INVALID.";
        }
        Long id = tokenService.getUserIdFromToken(token);
        String role = tokenService.getRoleFromToken(token);
        return "Token is VALID. UserID: " + id + ", Role: " + role;
    }
}
