package com.cabinet.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "file_records")
public class FileRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private long sizeBytes;
    private String md5;
    private Instant createdAt;
    private Instant updatedAt;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
