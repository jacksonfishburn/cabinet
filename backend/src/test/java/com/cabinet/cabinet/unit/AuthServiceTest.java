package com.cabinet.cabinet.unit;

import com.cabinet.entity.User;
import com.cabinet.exception.UnauthorizedException;
import com.cabinet.exception.UserAlreadyExistsException;
import com.cabinet.model.AuthRequest;
import com.cabinet.model.AuthResponse;
import com.cabinet.entity.Cabinet;
import com.cabinet.repository.UserRepository;
import com.cabinet.service.AuthService;
import com.cabinet.service.CabinetManagementService;
import com.cabinet.service.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private CabinetManagementService cabinetManagementService;

    @InjectMocks
    private AuthService authService;

    // Verifies a new user is persisted and a token is returned on successful registration.
    @Test
    void register_newUsername_returnsTokenAndSavesUser() {
        AuthRequest request = new AuthRequest("alice", "plain-password");
        Cabinet defaultCabinet = mock(Cabinet.class);
        when(defaultCabinet.getId()).thenReturn(100L);

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("plain-password")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(cabinetManagementService.createDefaultCabinet(any(User.class), eq("1"))).thenReturn(defaultCabinet);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("alice", response.username());
        assertEquals(100L, response.id());
        assertEquals("jwt-token", response.token());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("alice", userCaptor.getValue().getUsername());
        assertEquals("hashed-password", userCaptor.getValue().getPasswordHash());
    }

    // Verifies registration fails when the username is already taken.
    @Test
    void register_duplicateUsername_throwsConflict() {
        AuthRequest request = new AuthRequest("alice", "plain-password");
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any(User.class));
        verify(jwtService, never()).generateToken(any(User.class));
    }

    // Verifies password hashing is performed and the raw password is never persisted.
    @Test
    void register_passwordProvided_hashesPasswordBeforeSaving() {
        AuthRequest request = new AuthRequest("bob", "my-raw-password");
        Cabinet defaultCabinet = mock(Cabinet.class);
        when(defaultCabinet.getId()).thenReturn(101L);

        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(passwordEncoder.encode("my-raw-password")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });
        when(cabinetManagementService.createDefaultCabinet(any(User.class), eq("2"))).thenReturn(defaultCabinet);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        authService.register(request);

        verify(passwordEncoder).encode("my-raw-password");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("bcrypt-hash", userCaptor.getValue().getPasswordHash());
        assertTrue(!"my-raw-password".equals(userCaptor.getValue().getPasswordHash()));
    }

    // Verifies valid credentials return a JWT token.
    @Test
    void login_validCredentials_returnsToken() {
        AuthRequest request = new AuthRequest("carol", "correct-password");
        User existingUser = new User("carol", "stored-bcrypt-hash");
        existingUser.setId(2L);
        Cabinet defaultCabinet = mock(Cabinet.class);
        when(defaultCabinet.getId()).thenReturn(102L);

        when(userRepository.findByUsername("carol")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("correct-password", "stored-bcrypt-hash")).thenReturn(true);
        when(cabinetManagementService.getDefaultCabinet(existingUser)).thenReturn(defaultCabinet);
        when(jwtService.generateToken(existingUser)).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals(102L, response.id());
        assertEquals("carol", response.username());
        assertEquals("jwt-token", response.token());
    }

    // Verifies login fails when the password does not match.
    @Test
    void login_wrongPassword_throwsUnauthorized() {
        AuthRequest request = new AuthRequest("dave", "wrong-password");
        User existingUser = new User("dave", "stored-bcrypt-hash");

        when(userRepository.findByUsername("dave")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrong-password", "stored-bcrypt-hash")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(request));

        verify(jwtService, never()).generateToken(any(User.class));
    }

    // Verifies login fails when the username is not found.
    @Test
    void login_usernameNotFound_throwsUnauthorized() {
        AuthRequest request = new AuthRequest("missing-user", "any-password");
        when(userRepository.findByUsername("missing-user")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.login(request));

        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generateToken(any(User.class));
    }
}

