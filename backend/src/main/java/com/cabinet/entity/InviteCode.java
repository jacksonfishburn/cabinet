package com.cabinet.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "invite_codes")
public class InviteCode {

    protected InviteCode() {}

    public InviteCode(Cabinet cabinet, String code, Instant expiresAt) {
        this.cabinet = cabinet;
        this.code = code;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
        this.used = false;
    }

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
