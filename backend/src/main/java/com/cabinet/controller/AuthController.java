package com.cabinet.controller;

import com.cabinet.model.AuthRequest;
import com.cabinet.model.AuthResponse;
import com.cabinet.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody AuthRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse loginPost(@RequestBody AuthRequest request) {
        return authService.login(request);
    }

    @GetMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        return authService.login(request);
    }

    @DeleteMapping("/logout")
    public void logout(@RequestHeader("Authorization") String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        String token = header.substring(7);
        authService.logout(token);
    }
}