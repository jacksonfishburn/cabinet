package com.cabinet.model;

public record AuthResponse(
        Long defaultCabinetId,
        String username,
        String token
) {}