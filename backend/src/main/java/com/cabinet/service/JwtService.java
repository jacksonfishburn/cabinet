package com.cabinet.service;

import com.cabinet.entity.User;
import com.cabinet.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

	private final String secretValue;
	private final long expirationMs;
	private final Clock clock;
	private SecretKey signingKey;

	public JwtService(
			@Value("${jwt.secret}") String secretValue,
			@Value("${jwt.expiration-ms}") long expirationMs,
			Clock clock
	) {
		this.secretValue = secretValue;
		this.expirationMs = expirationMs;
		this.clock = clock;
	}

	@PostConstruct
	public void initialize() {
		byte[] keyBytes = decodeSecret(secretValue);
		if (keyBytes.length < 32) {
			throw new IllegalArgumentException("jwt.secret must resolve to at least 32 bytes for HS256");
		}
		this.signingKey = Keys.hmacShaKeyFor(keyBytes);
	}

	public String generateToken(User user) {
		Instant now = Instant.now(clock);
		Instant expiresAt = now.plusMillis(expirationMs);

		return Jwts.builder()
				.subject(user.getUsername())
				.claim("uid", user.getId())
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiresAt))
				.signWith(signingKey)
				.compact();
	}

	public String extractUsername(String token) {
		return getClaims(token).getSubject();
	}

	public Long extractUserId(String token) {
		Object userId = getClaims(token).get("uid");
		if (userId instanceof Number number) {
			return number.longValue();
		}
		if (userId instanceof String value && !value.isBlank()) {
			try {
				return Long.parseLong(value);
			} catch (NumberFormatException ex) {
				throw new UnauthorizedException("Missing or invalid JWT token");
			}
		}
		return null;
	}

	public boolean isTokenValid(String token, User user) {
		try {
			Claims claims = getClaims(token);
			return user != null
					&& user.getUsername().equals(claims.getSubject())
					&& claims.getExpiration() != null
					&& claims.getExpiration().toInstant().isAfter(Instant.now(clock));
		} catch (UnauthorizedException | JwtException | IllegalArgumentException ex) {
			return false;
		}
	}

	private Claims getClaims(String token) {
		try {
			return Jwts.parser()
					.setClock(() -> Date.from(Instant.now(clock)))
					.verifyWith(signingKey)
					.build()
					.parseSignedClaims(normalizeToken(token))
					.getPayload();
		} catch (JwtException | IllegalArgumentException ex) {
			throw new UnauthorizedException("Missing or invalid JWT token");
		}
	}

	private String normalizeToken(String token) {
		if (token == null || token.isBlank()) {
			throw new UnauthorizedException("Missing or invalid JWT token");
		}
		return token.startsWith("Bearer ") ? token.substring(7) : token;
	}

	private byte[] decodeSecret(String secret) {
		try {
			return Decoders.BASE64.decode(secret);
		} catch (Exception ex) {
			// Some decoder implementations throw different runtime exceptions (e.g. DecodingException).
			// Fall back to interpreting the configured secret as raw UTF-8 bytes.
			return secret.getBytes(StandardCharsets.UTF_8);
		}
	}
}
