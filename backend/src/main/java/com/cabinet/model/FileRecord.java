package com.cabinet.model;

import java.time.Instant;

public record FileRecord(
        long sizeBytes,
        String md5,
        Instant createdAt,
        Instant updatedAt
) {}
