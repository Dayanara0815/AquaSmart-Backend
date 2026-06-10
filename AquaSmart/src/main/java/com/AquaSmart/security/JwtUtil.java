package com.AquaSmart.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    private static final String SECRET_KEY = "aquasmart-super-secret-key-that-needs-to-be-secure-and-long-enough-for-hmac256-signature";
    private static final String ISSUER = "aquasmart-api";
    private static final long EXPIRATION_TIME_MS = 86400000; // 24 hours

    private final Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);

    public String generateToken(String email, String role, String fullName) {
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(email)
                .withClaim("email", email)
                .withClaim("role", role)
                .withClaim("fullName", fullName)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME_MS))
                .sign(algorithm);
    }

    public DecodedJWT validateToken(String token) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build();
        return verifier.verify(token);
    }

    public String getEmailFromToken(String token) {
        try {
            DecodedJWT jwt = validateToken(token);
            return jwt.getClaim("email").asString();
        } catch (JWTVerificationException e) {
            return null;
        }
    }
}
