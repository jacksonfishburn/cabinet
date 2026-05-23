package com.cabinet.model;

public record InsertResponse(
        String name,
        long sizeBytes,
        String md5
) {}
