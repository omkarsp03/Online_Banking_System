package com.onlinebanking.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.onlinebanking.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    private Algorithm algorithm() {
        if (jwtProperties.getSecret() == null || jwtProperties.getSecret().isBlank()) {
            throw new IllegalStateException("JWT_SECRET environment variable must be set");
        }
        return Algorithm.HMAC256(jwtProperties.getSecret().getBytes());
    }

    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + jwtProperties.getExpirationMs());
        return JWT.create()
                .withSubject(userDetails.getUsername())
                .withIssuer(jwtProperties.getIssuer())
                .withIssuedAt(now)
                .withExpiresAt(expiresAt)
                .withClaim("roles", userDetails.getAuthorities().stream().map(Object::toString).toList())
                .sign(algorithm());
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JWTVerificationException ex) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return getVerifier().verify(token).getSubject();
    }

    public boolean isTokenExpired(String token) {
        Date expiresAt = getVerifier().verify(token).getExpiresAt();
        return expiresAt.before(new Date());
    }

    private JWTVerifier getVerifier() {
        return JWT.require(algorithm())
                .withIssuer(jwtProperties.getIssuer())
                .build();
    }
}
