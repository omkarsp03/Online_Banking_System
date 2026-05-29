package com.onlinebanking.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinebanking.dto.AuthRequest;
import com.onlinebanking.dto.RegisterRequest;
import com.onlinebanking.dto.RoleChangeRequest;
import com.onlinebanking.entity.Role;
import com.onlinebanking.entity.RoleName;
import com.onlinebanking.entity.User;
import com.onlinebanking.repository.RoleRepository;
import com.onlinebanking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.onlinebanking.security.RateLimitingFilter;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AdminFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    private String adminToken;
    private User testCustomer;

    @BeforeEach
    void setUp() throws Exception {
        rateLimitingFilter.clearCache();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role customerRole = new Role();
        customerRole.setName(RoleName.ROLE_CUSTOMER);
        Role adminRole = new Role();
        adminRole.setName(RoleName.ROLE_ADMIN);
        roleRepository.saveAll(Set.of(customerRole, adminRole));

        // Create Admin
        User admin = new User();
        admin.setEmail("admin@example.com");
        admin.setPassword(passwordEncoder.encode("adminpass"));
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEnabled(true);
        admin.setRoles(Set.of(adminRole));
        userRepository.save(admin);

        // Login as Admin
        AuthRequest login = new AuthRequest("admin@example.com", "adminpass");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        adminToken = loginNode.at("/data/token").asText();

        // Create a test Customer to manage
        testCustomer = new User();
        testCustomer.setEmail("customer@example.com");
        testCustomer.setPassword(passwordEncoder.encode("password"));
        testCustomer.setFirstName("Customer");
        testCustomer.setLastName("One");
        testCustomer.setEnabled(true);
        testCustomer.setRoles(Set.of(customerRole));
        userRepository.save(testCustomer);
    }

    @Test
    void testAdminManagementFlow() throws Exception {
        // 1. List users (paginated)
        MvcResult listUsersResult = mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + adminToken)
                .param("page", "0")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode usersNode = objectMapper.readTree(listUsersResult.getResponse().getContentAsString());
        assertThat(usersNode.at("/data/content").size()).isEqualTo(2); // admin and customer

        // 2. Change customer role to admin
        RoleChangeRequest roleRequest = new RoleChangeRequest();
        roleRequest.setRoleName("ROLE_ADMIN");
        mockMvc.perform(put("/api/admin/users/" + testCustomer.getId() + "/role")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roleRequest)))
                .andExpect(status().isOk());

        // Verify role change
        User updatedCustomer = userRepository.findById(testCustomer.getId()).orElseThrow();
        assertThat(updatedCustomer.getRoles().iterator().next().getName()).isEqualTo(RoleName.ROLE_ADMIN);

        // 3. View audit logs
        mockMvc.perform(get("/api/admin/audit-logs")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
