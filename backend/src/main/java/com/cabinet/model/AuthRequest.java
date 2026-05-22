package com.cabinet.model;

public record AuthRequest(
        String username,
        String password
) {}
