package com.cabinet.model;

public record AuthResponse(
        Long id,
        String username,
        String token
) {}