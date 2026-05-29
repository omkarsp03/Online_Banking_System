package com.onlinebanking.auth;

import com.onlinebanking.config.JwtProperties;
import com.onlinebanking.dto.AuthRequest;
import com.onlinebanking.dto.AuthResponse;
import com.onlinebanking.dto.RegisterRequest;
import com.onlinebanking.entity.AuditEventType;
import com.onlinebanking.entity.Role;
import com.onlinebanking.entity.RoleName;
import com.onlinebanking.entity.User;
import com.onlinebanking.exception.ApiException;
import com.onlinebanking.repository.RoleRepository;
import com.onlinebanking.repository.UserRepository;
import com.onlinebanking.service.AuditLogService;
import com.onlinebanking.service.EmailService;
import com.onlinebanking.security.BankUserDetails;
import com.onlinebanking.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuditLogService auditLogService;
    private final EmailService emailService;

    @Transactional
    public AuthResponse register(RegisterRequest request, String remoteAddress) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException("A user with this email already exists");
        }

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new ApiException("Customer role is not configured"));

        User user = new User();
        user.setEmail(request.getEmail().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setEnabled(true);
        user.setRoles(Set.of(customerRole));

        User savedUser = userRepository.save(user);
        auditLogService.logEvent(AuditEventType.ACCOUNT_CREATED, "New customer registered", remoteAddress, savedUser);

        emailService.sendRegistrationEmail(savedUser.getEmail(), savedUser.getFirstName());

        String token = generateTokenFromUser(savedUser);
        return new AuthResponse(token, savedUser.getEmail(), customerRole.getName().name());
    }

    public AuthResponse login(AuthRequest request, String remoteAddress) {
        User user = userRepository.findByEmail(request.getUsername().toLowerCase()).orElse(null);
        if (user != null) {
            if (user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isAfter(Instant.now())) {
                throw new ApiException("Account is locked. Try again later.");
            } else if (user.getAccountLockedUntil() != null) {
                user.setAccountLockedUntil(null);
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
            }
        }

        try {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    request.getUsername().toLowerCase(), request.getPassword());
            Authentication authentication = authenticationManager.authenticate(authToken);
            BankUserDetails principal = (BankUserDetails) authentication.getPrincipal();
            
            if (user != null) {
                user.setFailedLoginAttempts(0);
                user.setAccountLockedUntil(null);
                userRepository.save(user);
                auditLogService.logEvent(AuditEventType.LOGIN, "User logged in", remoteAddress, user);
            }

            String token = jwtService.generateToken(principal);
            String role = principal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("ROLE_CUSTOMER");
            return new AuthResponse(token, principal.getUsername(), role);
        } catch (AuthenticationException ex) {
            if (user != null) {
                int attempts = user.getFailedLoginAttempts() + 1;
                user.setFailedLoginAttempts(attempts);
                if (attempts >= 5) {
                    user.setAccountLockedUntil(Instant.now().plus(15, ChronoUnit.MINUTES));
                    auditLogService.logEvent(AuditEventType.SECURITY_ALERT, "Account locked due to multiple failed login attempts", remoteAddress, user);
                }
                userRepository.save(user);
            }
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ApiException("User not found"));
    }

    private String generateTokenFromUser(User user) {
        BankUserDetails userDetails = new BankUserDetails(
                user.getEmail(),
                user.getPassword(),
                user.isEnabled(),
                user.getRoles().stream()
                        .map(role -> (GrantedAuthority) () -> role.getName().name())
                        .collect(Collectors.toSet())
        );
        return jwtService.generateToken(userDetails);
    }
}
