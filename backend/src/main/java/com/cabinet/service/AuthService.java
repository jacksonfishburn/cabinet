package com.cabinet.service;

import com.cabinet.entity.ApiToken;
import com.cabinet.entity.User;
import com.cabinet.exception.UnauthorizedException;
import com.cabinet.exception.UserAlreadyExistsException;
import com.cabinet.model.AuthRequest;
import com.cabinet.model.AuthResponse;
import com.cabinet.repository.TokenRepository;
import com.cabinet.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final TokenRepository tokenRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository, PasswordEncoder encoder, TokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.tokenRepository = tokenRepository;
    }

    public AuthResponse register(AuthRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException(request.username());
        }
        String encodedPassword = encoder.encode(request.password());
        User user = new User(request.username(), encodedPassword);
        userRepository.save(user);

        String token = createAndStoreTokenFor(user);
        return new AuthResponse(user.getId(), user.getUsername(), token);
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UnauthorizedException("User '" + request.username() + "' not found"));
        if (!encoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid password for user '" + request.username() + "'");
        }
        String token = createAndStoreTokenFor(user);
        return new AuthResponse(user.getId(), user.getUsername(), token);
    }

    public void logout(String token) {
        ApiToken apiToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Not logged in."));
        tokenRepository.delete(apiToken);
    }

    private String createAndStoreTokenFor(User user) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = Instant.now();

        // default token expiry: 30 days
        long tokenExpirySeconds = 30L * 24L * 60L * 60L;

        Instant expiresAt = now.plusSeconds(tokenExpirySeconds);
        ApiToken apiToken = new ApiToken(token, user, expiresAt, false, now);
        tokenRepository.save(apiToken);
        return token;
    }
}