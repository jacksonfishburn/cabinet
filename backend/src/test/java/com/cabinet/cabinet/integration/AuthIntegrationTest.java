package com.cabinet.cabinet.integration;

import com.cabinet.model.AuthRequest;
import com.cabinet.repository.CabinetRepository;
import com.cabinet.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.matchesRegex;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Testcontainers
class AuthIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine");

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CabinetRepository cabinetRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        cabinetRepository.deleteAll();
        userRepository.deleteAll();
        mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    // Verifies register happy path returns 200 with a non-empty token.
    @Test
    void register_validRequest_returns200AndNonEmptyToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("alice", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.token", matchesRegex(".+")));
    }

    // Verifies register returns 409 when username already exists.
    @Test
    void register_duplicateUsername_returns409() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("alice", "password123"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("alice", "another-password"))))
                .andExpect(status().isConflict());
    }

    // Verifies register returns 400 when username is missing.
    @Test
    void register_missingUsername_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest(null, "password123"))))
                .andExpect(status().isBadRequest());
    }

    // Verifies register returns 400 when password is missing.
    @Test
    void register_missingPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("alice", null))))
                .andExpect(status().isBadRequest());
    }

    // Verifies login happy path returns 200 with a non-empty token.
    @Test
    void login_validCredentials_returns200AndNonEmptyToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("bob", "password123"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("bob", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("bob"))
                .andExpect(jsonPath("$.token", matchesRegex(".+")));
    }

    // Verifies login returns 401 when password is wrong.
    @Test
    void login_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("carol", "password123"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("carol", "wrong-password"))))
                .andExpect(status().isUnauthorized());
    }

    // Verifies login returns 401 when username does not exist.
    @Test
    void login_usernameDoesNotExist_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("missing-user", "password123"))))
                .andExpect(status().isUnauthorized());
    }

    // Verifies login returns 400 when credentials are missing.
    @Test
    void login_missingCredentials_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
