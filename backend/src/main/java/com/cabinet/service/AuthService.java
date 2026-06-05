package com.cabinet.service;

import com.cabinet.entity.Cabinet;
import com.cabinet.entity.User;
import com.cabinet.exception.UnauthorizedException;
import com.cabinet.exception.UserAlreadyExistsException;
import com.cabinet.model.AuthRequest;
import com.cabinet.model.AuthResponse;
import com.cabinet.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final CabinetManagementService cabinetManagementService;

    public AuthService(UserRepository userRepository, PasswordEncoder encoder,
                       JwtService jwtService, CabinetManagementService cabinetManagementService) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.cabinetManagementService = cabinetManagementService;
    }

    @Transactional
    public AuthResponse register(AuthRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException(request.username());
        }
        String encodedPassword = encoder.encode(request.password());
        User user = new User(request.username(), encodedPassword);
        userRepository.save(user);

        Cabinet defaultCabinet = cabinetManagementService.createDefaultCabinet(user, user.getId().toString());

        String token = jwtService.generateToken(user);
        return new AuthResponse(defaultCabinet.getId(), user.getUsername(), token);
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UnauthorizedException("User '" + request.username() + "' not found"));
        if (!encoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid password for user '" + request.username() + "'");
        }

        Cabinet defaultCabinet = cabinetManagementService.getDefaultCabinet(user);

        String token = jwtService.generateToken(user);
        return new AuthResponse(defaultCabinet.getId(), user.getUsername(), token);
    }
}