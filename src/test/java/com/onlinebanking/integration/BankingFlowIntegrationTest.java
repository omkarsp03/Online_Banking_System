package com.onlinebanking.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinebanking.dto.*;
import com.onlinebanking.entity.*;
import com.onlinebanking.repository.BankAccountRepository;
import com.onlinebanking.repository.RoleRepository;
import com.onlinebanking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.onlinebanking.security.RateLimitingFilter;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class BankingFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    private String userToken;
    private String userEmail = "jane@example.com";

    @BeforeEach
    void setUp() throws Exception {
        rateLimitingFilter.clearCache();
        bankAccountRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role customerRole = new Role();
        customerRole.setName(RoleName.ROLE_CUSTOMER);
        Role adminRole = new Role();
        adminRole.setName(RoleName.ROLE_ADMIN);
        roleRepository.saveAll(Set.of(customerRole, adminRole));

        // Register
        RegisterRequest register = new RegisterRequest(
                userEmail, "password123", "Jane", "Doe", "+1234567890"
        );
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        // Login
        AuthRequest login = new AuthRequest(userEmail, "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        userToken = loginNode.at("/data/token").asText();
    }

    @Test
    void testBankingLifecycle() throws Exception {
        // 1. Create Checking Account
        CreateAccountRequest createChecking = new CreateAccountRequest(AccountType.CHECKING, "USD");
        MvcResult checkingResult = mockMvc.perform(post("/api/customer/accounts")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createChecking)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode checkingNode = objectMapper.readTree(checkingResult.getResponse().getContentAsString());
        String checkingNum = checkingNode.at("/data/accountNumber").asText();
        assertThat(checkingNum).isNotBlank();

        // 2. Deposit into Checking
        AmountRequest deposit = new AmountRequest(new BigDecimal("500.00"), "USD", "Deposit cash");
        mockMvc.perform(post("/api/customer/accounts/" + checkingNum + "/deposit")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deposit)))
                .andExpect(status().isOk());

        // 3. Create Savings Account
        CreateAccountRequest createSavings = new CreateAccountRequest(AccountType.SAVINGS, "USD");
        MvcResult savingsResult = mockMvc.perform(post("/api/customer/accounts")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createSavings)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode savingsNode = objectMapper.readTree(savingsResult.getResponse().getContentAsString());
        String savingsNum = savingsNode.at("/data/accountNumber").asText();

        // 4. Transfer Checking -> Savings
        TransactionRequest transfer = new TransactionRequest(checkingNum, savingsNum, new BigDecimal("200.00"), "USD", "Save money");
        mockMvc.perform(post("/api/customer/transactions")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transfer)))
                .andExpect(status().isOk());

        // 5. Verify balances
        MvcResult checkBalanceChecking = mockMvc.perform(get("/api/customer/accounts/" + checkingNum + "/balance")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();
        BigDecimal checkingBal = new BigDecimal(objectMapper.readTree(checkBalanceChecking.getResponse().getContentAsString()).at("/data").asText());
        assertThat(checkingBal).isEqualByComparingTo(new BigDecimal("300.00"));

        MvcResult checkBalanceSavings = mockMvc.perform(get("/api/customer/accounts/" + savingsNum + "/balance")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();
        BigDecimal savingsBal = new BigDecimal(objectMapper.readTree(checkBalanceSavings.getResponse().getContentAsString()).at("/data").asText());
        assertThat(savingsBal).isEqualByComparingTo(new BigDecimal("200.00"));

        // 6. Export Statement (PDF format)
        mockMvc.perform(get("/api/customer/accounts/" + checkingNum + "/statement")
                .header("Authorization", "Bearer " + userToken)
                .param("format", "pdf"))
                .andExpect(status().isOk());
    }

    @Test
    void testBeneficiaryFlow() throws Exception {
        BeneficiaryRequest request = new BeneficiaryRequest("Bob Smith", "987654321", "Chase Bank");
        
        // Add Beneficiary
        MvcResult addResult = mockMvc.perform(post("/api/customer/beneficiaries")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode addedNode = objectMapper.readTree(addResult.getResponse().getContentAsString());
        Long beneficiaryId = addedNode.at("/data/id").asLong();
        String statusVal = addedNode.at("/data/status").asText();
        assertThat(statusVal).isEqualTo("PENDING");

        // Verify Beneficiary
        mockMvc.perform(post("/api/customer/beneficiaries/" + beneficiaryId + "/verify")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // List Beneficiaries
        MvcResult listResult = mockMvc.perform(get("/api/customer/beneficiaries")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode listNode = objectMapper.readTree(listResult.getResponse().getContentAsString());
        assertThat(listNode.at("/data").size()).isGreaterThan(0);
        assertThat(listNode.at("/data/0/status").asText()).isEqualTo("ACTIVE");
    }
}
