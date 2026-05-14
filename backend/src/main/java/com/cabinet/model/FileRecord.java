package com.cabinet.model;

import java.time.Instant;

public record FileRecord(
        String name,
        long sizeBytes,
        String md5,
        Instant createdAt,
        Instant updatedAt
) {}
