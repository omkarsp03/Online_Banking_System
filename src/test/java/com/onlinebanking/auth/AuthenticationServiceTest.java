package com.onlinebanking.auth;

import com.onlinebanking.config.JwtProperties;
import com.onlinebanking.dto.AuthRequest;
import com.onlinebanking.dto.AuthResponse;
import com.onlinebanking.dto.RegisterRequest;
import com.onlinebanking.entity.Role;
import com.onlinebanking.entity.RoleName;
import com.onlinebanking.entity.User;
import com.onlinebanking.repository.AuditLogRepository;
import com.onlinebanking.repository.RoleRepository;
import com.onlinebanking.repository.UserRepository;
import com.onlinebanking.security.BankUserDetails;
import com.onlinebanking.security.JwtService;
import com.onlinebanking.service.AuditLogService;
import com.onlinebanking.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private EmailService emailService;

    private AuditLogService auditLogService;
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        auditLogService = new AuditLogService(auditLogRepository, userRepository);
        authenticationService = new AuthenticationService(userRepository, roleRepository, passwordEncoder,
                authenticationManager, jwtService, jwtProperties, auditLogService, emailService);
    }

    @Test
    void register_createsCustomerAndReturnsToken() {
        RegisterRequest request = new RegisterRequest(
                "alice@example.com",
                "password123",
                "Alice",
                "Wonderland",
                "+12345678901"
        );

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        Role customerRole = new Role();
        customerRole.setId(1L);
        customerRole.setName(RoleName.ROLE_CUSTOMER);
        when(roleRepository.findByName(RoleName.ROLE_CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(5L);
            return saved;
        });
        when(jwtService.generateToken(any(BankUserDetails.class))).thenReturn("token123");

        AuthResponse response = authenticationService.register(request, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("token123");
        assertThat(response.getUsername()).isEqualTo("alice@example.com");
        assertThat(response.getRole()).isEqualTo("ROLE_CUSTOMER");
    }

    @Test
    void login_withValidCredentials_returnsAuthResponse() {
        AuthRequest request = new AuthRequest("bob@example.com", "test-password");
        Authentication authentication = mock(Authentication.class);
        BankUserDetails userDetails = new BankUserDetails("bob@example.com", "encoded", true, Set.of(() -> "ROLE_CUSTOMER"));

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        User persistent = new User();
        persistent.setEmail("bob@example.com");
        persistent.setPassword("encoded");
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(persistent));
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        AuthResponse response = authenticationService.login(request, "127.0.0.1");

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUsername()).isEqualTo("bob@example.com");
        assertThat(response.getRole()).isEqualTo("ROLE_CUSTOMER");
    }

    @Test
    void login_withInvalidCredentials_throwsBadCredentialsException() {
        AuthRequest request = new AuthRequest("bob@example.com", "wrong-password");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Bad credentials"));

        assertThrows(RuntimeException.class, () -> authenticationService.login(request, "127.0.0.1"));
    }
}
