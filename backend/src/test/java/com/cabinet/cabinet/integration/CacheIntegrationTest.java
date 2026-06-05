package com.cabinet.cabinet.integration;

import com.cabinet.model.AuthRequest;
import com.cabinet.model.AuthResponse;
import com.cabinet.model.ListCabinetsResponse;
import com.cabinet.repository.CabinetRepository;
import com.cabinet.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import jakarta.servlet.Filter;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Testcontainers
class CacheIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CabinetRepository cabinetRepository;

    @BeforeEach
    void setUp() {
        cabinetRepository.deleteAll();
        userRepository.deleteAll();
        Filter springSecurityFilterChain = webApplicationContext.getBean("springSecurityFilterChain", Filter.class);
        mockMvc = webAppContextSetup(webApplicationContext).addFilters(springSecurityFilterChain).build();
    }

    @Test
    void peek_firstCall_hitsDatabaseAndPopulatesCache() throws Exception {
        UserAndCabinet uc = registerUserAndGetCabinet("alice");

        mockMvc.perform(get("/api/peek/" + uc.cabinetId).header("Authorization", "Bearer " + uc.auth.token()))
                .andExpect(status().isOk());
    }

    @Test
    void peek_secondCall_returnsCachedResultWithoutHittingDatabase() throws Exception {
        UserAndCabinet uc = registerUserAndGetCabinet("bob");

        mockMvc.perform(get("/api/peek/" + uc.cabinetId).header("Authorization", "Bearer " + uc.auth.token()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/peek/" + uc.cabinetId).header("Authorization", "Bearer " + uc.auth.token()))
                .andExpect(status().isOk());
    }

    @Test
    void insert_afterPeek_evictsCacheEntry() throws Exception {
        UserAndCabinet uc = registerUserAndGetCabinet("carol");

        mockMvc.perform(get("/api/peek/" + uc.cabinetId).header("Authorization", "Bearer " + uc.auth.token()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/" + uc.cabinetId + "/sample")
                        .header("Authorization", "Bearer " + uc.auth.token())
                        .contentType(APPLICATION_OCTET_STREAM)
                        .content(new byte[]{1, 2, 3}))
                .andExpect(status().isOk());
    }

    @Test
    void delete_afterPeek_evictsCacheEntry() throws Exception {
        UserAndCabinet uc = registerUserAndGetCabinet("dave");

        mockMvc.perform(post("/api/" + uc.cabinetId + "/sample")
                        .header("Authorization", "Bearer " + uc.auth.token())
                        .contentType(APPLICATION_OCTET_STREAM)
                        .content(new byte[]{1, 2, 3}))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/peek/" + uc.cabinetId).header("Authorization", "Bearer " + uc.auth.token()))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/" + uc.cabinetId + "/sample").header("Authorization", "Bearer " + uc.auth.token()))
                .andExpect(status().isNoContent());
    }

    private UserAndCabinet registerUserAndGetCabinet(String username) throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest(username, "password123"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        AuthResponse auth = objectMapper.readValue(response, AuthResponse.class);

        String listResponse = mockMvc.perform(get("/api/list")
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        ListCabinetsResponse cabinets = objectMapper.readValue(listResponse, ListCabinetsResponse.class);

        Long cabinetId = cabinets.cabinets().get(0).id();
        return new UserAndCabinet(auth, cabinetId);
    }

    private record UserAndCabinet(AuthResponse auth, Long cabinetId) {}
}





