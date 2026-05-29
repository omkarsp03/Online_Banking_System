package com.onlinebanking.controller;

import com.onlinebanking.auth.AuthenticationService;
import com.onlinebanking.dto.ApiResponse;
import com.onlinebanking.dto.AuthRequest;
import com.onlinebanking.dto.AuthResponse;
import com.onlinebanking.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration and authentication")
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new customer account and generates a JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request,
                                                              HttpServletRequest servletRequest) {
        AuthResponse authResponse = authenticationService.register(request, servletRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        Instant.now(),
                        HttpStatus.CREATED.value(),
                        "Registration successful",
                        authResponse
                ));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Verifies credentials and returns a JWT token. Automatically locks account for 15 minutes after 5 failed attempts.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request,
                                                           HttpServletRequest servletRequest) {
        AuthResponse authResponse = authenticationService.login(request, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(new ApiResponse<>(
                Instant.now(),
                HttpStatus.OK.value(),
                "Authentication successful",
                authResponse
        ));
    }
}
