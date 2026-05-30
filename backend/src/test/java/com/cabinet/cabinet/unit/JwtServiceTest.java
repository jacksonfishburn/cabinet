package com.cabinet.cabinet.unit;

import com.cabinet.entity.User;
import com.cabinet.exception.UnauthorizedException;
import com.cabinet.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import io.jsonwebtoken.io.Decoders;

import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTest {

	private final String secret = "IIsecretIIjwtIIkeyIIforIItestIIauthenticationII1234567890";
	private final long expirationMs = 3_600_000L;

	@Test
	public void generateToken_and_decode_shouldContainUsername() {
		Instant now = Instant.parse("2026-05-29T12:00:00Z");
		Clock clock = Clock.fixed(now, ZoneOffset.UTC);

		JwtService jwtService = new JwtService(secret, expirationMs, clock);
		jwtService.initialize();

		User user = new User("alice", "pw");
		user.setId(42L);

		String token = jwtService.generateToken(user);

		byte[] keyBytes;
		try {
			keyBytes = Decoders.BASE64.decode(secret);
		} catch (Exception e) {
			keyBytes = secret.getBytes(StandardCharsets.UTF_8);
		}
		SecretKey key = Keys.hmacShaKeyFor(keyBytes);
		Claims claims = Jwts.parser()
				.setClock(() -> java.util.Date.from(now))
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();

		assertEquals("alice", claims.getSubject());
	}

	@Test
	public void generateToken_expiryMatchesConfiguredDuration() {
		Instant now = Instant.parse("2026-05-29T12:00:00Z");
		Clock clock = Clock.fixed(now, ZoneOffset.UTC);

		JwtService jwtService = new JwtService(secret, expirationMs, clock);
		jwtService.initialize();

		User user = new User("bob", "pw");
		user.setId(7L);

		String token = jwtService.generateToken(user);

		byte[] keyBytes;
		try {
			keyBytes = Decoders.BASE64.decode(secret);
		} catch (Exception e) {
			keyBytes = secret.getBytes(StandardCharsets.UTF_8);
		}
		SecretKey key = Keys.hmacShaKeyFor(keyBytes);
		Claims claims = Jwts.parser()
				.setClock(() -> java.util.Date.from(now))
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();

		assertNotNull(claims.getExpiration());
		assertEquals(now.plusMillis(expirationMs), claims.getExpiration().toInstant());
	}

	@Test
	public void validate_validUnexpiredToken_returnsTrue() {
		Instant now = Instant.parse("2026-05-29T12:00:00Z");
		Clock clock = Clock.fixed(now, ZoneOffset.UTC);

		JwtService jwtService = new JwtService(secret, expirationMs, clock);
		jwtService.initialize();

		User user = new User("carol", "pw");
		user.setId(9L);

		String token = jwtService.generateToken(user);

		assertTrue(jwtService.isTokenValid(token, user));
	}

	@Test
	public void validate_expiredToken_returnsFalse() {
		// Generate token at time T0
		Instant t0 = Instant.parse("2026-05-29T09:00:00Z");
		Clock genClock = Clock.fixed(t0, ZoneOffset.UTC);
		JwtService genService = new JwtService(secret, expirationMs, genClock);
		genService.initialize();

		User user = new User("dave", "pw");
		user.setId(11L);

		String token = genService.generateToken(user);

		// Validate at time after expiry (T0 + 2h)
		Instant later = t0.plusSeconds(2 * 3600);
		Clock valClock = Clock.fixed(later, ZoneOffset.UTC);
		JwtService valService = new JwtService(secret, expirationMs, valClock);
		valService.initialize();

		assertFalse(valService.isTokenValid(token, user));
	}

	@Test
	public void validate_tamperedPayload_returnsFalse() {
		Instant now = Instant.parse("2026-05-29T12:00:00Z");
		Clock clock = Clock.fixed(now, ZoneOffset.UTC);

		JwtService jwtService = new JwtService(secret, expirationMs, clock);
		jwtService.initialize();

		User user = new User("ellen", "pw");
		user.setId(13L);

		String token = jwtService.generateToken(user);

		// Tamper with the payload (middle segment)
		String[] parts = token.split("\\.");
		assertEquals(3, parts.length);
		String payload = parts[1];
		char[] chars = payload.toCharArray();
		int idx = Math.min(5, chars.length - 1);
		chars[idx] = chars[idx] == 'A' ? 'B' : 'A';
		parts[1] = new String(chars);
		String tampered = String.join(".", parts);

		assertFalse(jwtService.isTokenValid(tampered, user));
	}

	@Test
	public void validate_garbageString_returnsFalse() {
		Instant now = Instant.parse("2026-05-29T12:00:00Z");
		Clock clock = Clock.fixed(now, ZoneOffset.UTC);

		JwtService jwtService = new JwtService(secret, expirationMs, clock);
		jwtService.initialize();

		User user = new User("frank", "pw");
		user.setId(15L);

		assertFalse(jwtService.isTokenValid("not-a-jwt", user));
	}

	@Test
	public void extractUsername_validToken_returnsUsername() {
		Instant now = Instant.parse("2026-05-29T12:00:00Z");
		Clock clock = Clock.fixed(now, ZoneOffset.UTC);

		JwtService jwtService = new JwtService(secret, expirationMs, clock);
		jwtService.initialize();

		User user = new User("gwen", "pw");
		user.setId(17L);

		String token = jwtService.generateToken(user);

		assertEquals("gwen", jwtService.extractUsername(token));
	}

	@Test
	public void extractUsername_invalidToken_throwsHandledException() {
		Instant now = Instant.parse("2026-05-29T12:00:00Z");
		Clock clock = Clock.fixed(now, ZoneOffset.UTC);

		JwtService jwtService = new JwtService(secret, expirationMs, clock);
		jwtService.initialize();

		assertThrows(UnauthorizedException.class, () -> jwtService.extractUsername("definitely-garbage"));
	}
}




