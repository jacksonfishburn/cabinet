package com.cabinet.cabinet.integration;

import com.cabinet.model.AuthRequest;
import com.cabinet.model.AuthResponse;
import com.cabinet.repository.FileRecordRepository;
import com.cabinet.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
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
import static org.mockito.Mockito.times;
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

    @MockitoSpyBean
    private FileRecordRepository fileRecordRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        Cache cache = cacheManager.getCache("peek");
        if (cache != null) {
            cache.clear();
        }
        Mockito.clearInvocations(fileRecordRepository);
        Filter springSecurityFilterChain = webApplicationContext.getBean("springSecurityFilterChain", Filter.class);
        mockMvc = webAppContextSetup(webApplicationContext).addFilters(springSecurityFilterChain).build();
    }

    @Test
    void peek_firstCall_hitsDatabaseAndPopulatesCache() throws Exception {
        AuthResponse auth = registerUser("alice");
        Cache cache = cacheManager.getCache("peek");

        mockMvc.perform(get("/api/peek").header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk());

        Mockito.verify(fileRecordRepository, times(1)).findByUserId(auth.id());
        assertNotNull(cache);
        assertNotNull(cache.get(auth.id()));
    }

    @Test
    void peek_secondCall_returnsCachedResultWithoutHittingDatabase() throws Exception {
        AuthResponse auth = registerUser("bob");
        Cache cache = cacheManager.getCache("peek");

        mockMvc.perform(get("/api/peek").header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/peek").header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk());

        Mockito.verify(fileRecordRepository, times(1)).findByUserId(auth.id());
        assertNotNull(cache);
        assertNotNull(cache.get(auth.id()));
    }

    @Test
    void insert_afterPeek_evictsCacheEntry() throws Exception {
        AuthResponse auth = registerUser("carol");
        Cache cache = cacheManager.getCache("peek");

        mockMvc.perform(get("/api/peek").header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk());
        assertNotNull(cache);
        assertNotNull(cache.get(auth.id()));

        mockMvc.perform(post("/api/sample")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(APPLICATION_OCTET_STREAM)
                        .content(new byte[]{1, 2, 3}))
                .andExpect(status().isOk());

        assertNull(cache.get(auth.id()));
    }

    @Test
    void delete_afterPeek_evictsCacheEntry() throws Exception {
        AuthResponse auth = registerUser("dave");
        Cache cache = cacheManager.getCache("peek");

        mockMvc.perform(post("/api/sample")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(APPLICATION_OCTET_STREAM)
                        .content(new byte[]{1, 2, 3}))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/peek").header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk());
        assertNotNull(cache);
        assertNotNull(cache.get(auth.id()));

        mockMvc.perform(delete("/api/sample").header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isNoContent());

        assertNull(cache.get(auth.id()));
    }

    private AuthResponse registerUser(String username) throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest(username, "password123"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(response, AuthResponse.class);
    }
}





