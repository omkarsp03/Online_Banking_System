package com.onlinebanking.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinebanking.dto.AuthRequest;
import com.onlinebanking.dto.RegisterRequest;
import com.onlinebanking.entity.Role;
import com.onlinebanking.entity.RoleName;
import com.onlinebanking.repository.RoleRepository;
import com.onlinebanking.repository.UserRepository;
import com.onlinebanking.security.RateLimitingFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() {
        rateLimitingFilter.clearCache();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        Role customerRole = new Role();
        customerRole.setName(RoleName.ROLE_CUSTOMER);
        roleRepository.save(customerRole);
    }

    @Test
    void testRegisterAndLoginFlow() throws Exception {
        RegisterRequest register = new RegisterRequest(
                "testuser@example.com",
                "password123",
                "Test",
                "User",
                "+1234567890"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        AuthRequest login = new AuthRequest("testuser@example.com", "password123");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk());
    }

    @Test
    void testAccountLockoutAfterFiveFailedAttempts() throws Exception {
        RegisterRequest register = new RegisterRequest(
                "lockeduser@example.com",
                "password123",
                "Locked",
                "User",
                "+1234567890"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        // Perform 5 failed login attempts from different IPs so they don't trigger the rate limiting filter (which limits by IP), but they do trigger account lockout (which locks by username)
        AuthRequest badLogin = new AuthRequest("lockeduser@example.com", "wrongpassword");
        for (int i = 0; i < 5; i++) {
            final int ipSuffix = i;
            mockMvc.perform(post("/api/auth/login")
                    .with(request -> {
                        request.setRemoteAddr("192.168.1." + ipSuffix);
                        return request;
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(badLogin)))
                    .andExpect(status().isUnauthorized());
        }

        // The 6th attempt should be locked (returns HttpStatus.BAD_REQUEST via global exception handler)
        mockMvc.perform(post("/api/auth/login")
                .with(request -> {
                    request.setRemoteAddr("192.168.1.99");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRateLimitingOnLoginEndpoint() throws Exception {
        AuthRequest login = new AuthRequest("rate@example.com", "password");

        // The bucket has capacity 5. Let's make 6 login attempts rapidly from same IP.
        // Note: MockMvc uses same request context by default, representing the same client/IP.
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(login)));
        }

        // 6th request should trigger 429 Too Many Requests
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isTooManyRequests());
    }
}
