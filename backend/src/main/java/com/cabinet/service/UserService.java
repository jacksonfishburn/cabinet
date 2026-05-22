package com.cabinet.service;

import com.cabinet.entity.User;
import com.cabinet.model.AuthRequest;
import com.cabinet.model.AuthResponse;
import com.cabinet.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository, PasswordEncoder encoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(AuthRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new RuntimeException("Username already exists");
        }
        String encodedPassword = encoder.encode(request.password());
        User user = new User(request.username(), encodedPassword);
        userRepository.save(user);
        String token = jwtService.generate(user);
        return new AuthResponse(user.getId(), user.getUsername(), token);
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!encoder.matches(request.password(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }
        String token = jwtService.generate(user);
        return new AuthResponse(user.getId(), user.getUsername(), token);
    }
}