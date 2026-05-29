package com.onlinebanking.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinebanking.entity.Role;
import com.onlinebanking.entity.RoleName;
import com.onlinebanking.entity.User;
import com.onlinebanking.repository.RoleRepository;
import com.onlinebanking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.onlinebanking.security.RateLimitingFilter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() {
        rateLimitingFilter.clearCache();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        Role customerRole = new Role();
        customerRole.setName(RoleName.ROLE_CUSTOMER);
        Role adminRole = new Role();
        adminRole.setName(RoleName.ROLE_ADMIN);
        roleRepository.saveAll(Set.of(customerRole, adminRole));
    }

    @Test
    void registerLoginAndCustomerAccountFlow() throws Exception {
        String registerBody = objectMapper.writeValueAsString(new com.onlinebanking.dto.RegisterRequest(
                "jane@example.com",
                "password123",
                "Jane",
                "Doe",
                "+12345678901"
        ));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(new com.onlinebanking.dto.AuthRequest("jane@example.com", "password123"));
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = loginJson.at("/data/token").asText();
        assertThat(token).isNotBlank();

        String accountBody = objectMapper.writeValueAsString(new com.onlinebanking.dto.CreateAccountRequest(com.onlinebanking.entity.AccountType.CHECKING, "USD"));
        MvcResult accountResult = mockMvc.perform(post("/api/customer/accounts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(accountBody))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode accountJson = objectMapper.readTree(accountResult.getResponse().getContentAsString());
        String accountNumber = accountJson.at("/data/accountNumber").asText();
        assertThat(accountNumber).isNotBlank();

        String depositBody = objectMapper.writeValueAsString(new com.onlinebanking.dto.AmountRequest(new java.math.BigDecimal("100"), "USD", "Initial deposit"));
        mockMvc.perform(post("/api/customer/accounts/" + accountNumber + "/deposit")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(depositBody))
                .andExpect(status().isOk());

        String balanceResult = mockMvc.perform(get("/api/customer/accounts/" + accountNumber + "/balance")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode balanceJson = objectMapper.readTree(balanceResult);
        assertThat(new java.math.BigDecimal(balanceJson.at("/data").asText())).isEqualByComparingTo(new java.math.BigDecimal("100"));
    }

    @Test
    void adminCanViewAuditLogsAndChangeUserRole() throws Exception {
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();
        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow();

        User admin = new User();
        admin.setEmail("admin@example.com");
        admin.setPassword(passwordEncoder.encode("adminpass"));
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEnabled(true);
        admin.setRoles(Set.of(adminRole));
        userRepository.save(admin);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new com.onlinebanking.dto.AuthRequest("admin@example.com", "adminpass"))))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).at("/data/token").asText();

        mockMvc.perform(get("/api/admin/audit-logs")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        User customer = new User();
        customer.setEmail("customer2@example.com");
        customer.setPassword(passwordEncoder.encode("customerpass"));
        customer.setFirstName("Customer");
        customer.setLastName("Two");
        customer.setEnabled(true);
        customer.setRoles(Set.of(customerRole));
        userRepository.save(customer);

        String roleChangeBody = objectMapper.writeValueAsString(new com.onlinebanking.dto.RoleChangeRequest());
        // use a JSON object because RoleChangeRequest has a single property
        roleChangeBody = "{\"roleName\":\"ROLE_ADMIN\"}";

        mockMvc.perform(put("/api/admin/users/" + customer.getId() + "/role")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(roleChangeBody))
                .andExpect(status().isOk());
    }
}
