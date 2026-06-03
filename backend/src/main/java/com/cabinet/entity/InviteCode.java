package com.cabinet.entity;

import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;

@Entity
@Table(name = "invite_codes")
public class InviteCode {

    protected InviteCode() {}

    public InviteCode(Cabinet cabinet, String code) {
        this.cabinet = cabinet;
        this.code = code;
        Duration expiration = Duration.ofMillis(expirationMs);
        this.createdAt = Instant.now();
        this.expiresAt = this.createdAt.plus(expiration);
        this.used = false;
    }

    @Value("${cabinet.code-expiration-ms}")
    private Long expirationMs;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cabinet_id", nullable = false)
    private Cabinet cabinet;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used;

    public Long getId() { return id; }

    public Cabinet getCabinet() { return cabinet; }

    public String getCode() { return code; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getExpiresAt() { return expiresAt; }

    public boolean isUsed() { return used; }

    public void markUsed() { this.used = true; }

}
