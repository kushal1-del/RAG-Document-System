package com.kushal.rag.service;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/**
 * Handles everything related to JWT tokens:
 *   1. Creating a signed token when a user logs in.
 *   2. Reading/validating a token when a request comes in
 *      with an "Authorization: Bearer <token>" header.
 *
 * A JWT has 3 parts: header.payload.signature
 *   - header    -> algorithm info (handled automatically by jjwt)
 *   - payload   -> our data: email (subject), role, issued/expiry times
 *   - signature -> proves the token wasn't tampered with, using our secret key
 */
@Service
public class JwtService {

    // The secret key used to SIGN and VERIFY tokens.
    // Must be a SecretKey (not just any Key) for jjwt 0.12.x's
    // verifyWith(SecretKey) method to accept it.
    private final SecretKey signingKey;

    // How long a token stays valid after being issued (in milliseconds).
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration.ms}") long expirationMs) {

        // Converts our plain-text secret string into a proper
        // cryptographic SecretKey object that HS256 can use.
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());

        this.expirationMs = expirationMs;
    }

    // =========================================================
    // CREATE TOKEN (used during login)
    // =========================================================

    /**
     * Builds a signed JWT containing:
     *   - subject = user's email (used to identify who this token belongs to)
     *   - a custom claim "role" = USER or ADMIN (used later for authorization)
     *   - issuedAt / expiration timestamps
     *
     * The token is signed with our secret key, so if anyone tries to
     * modify the payload (e.g. change role to ADMIN), the signature
     * check will fail and the token will be rejected.
     */
    public String generateToken(String email, String role) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // =========================================================
    // READ / VALIDATE TOKEN (used on every protected request)
    // =========================================================

    /**
     * Parses the token and verifies its signature using our secret key.
     *
     * jjwt 0.12.x parsing chain:
     *   Jwts.parser()              -> get a parser builder
     *       .verifyWith(signingKey) -> tell it which key to verify the signature with
     *       .build()                -> finalize into a usable JwtParser
     *       .parseSignedClaims(token) -> actually parse + verify the token
     *
     * If the token was tampered with, expired, or malformed, this
     * throws an exception (JwtException / ExpiredJwtException / etc.)
     * — which is exactly what isTokenValid() below catches.
     *
     * .getPayload() gives us the actual Claims (email, role, etc.)
     * that we stored when the token was created.
     */
    public Claims extractClaims(String token) {

        Jws<Claims> jws =
                Jwts.parser()
                        .verifyWith(signingKey)
                        .build()
                        .parseSignedClaims(token);

        return jws.getPayload();
    }

    /**
     * Convenience method: pulls just the email (the "subject" field)
     * out of a token's claims.
     */
    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Convenience method: pulls the custom "role" claim out of the token.
     */
    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    /**
     * Returns true if the token is well-formed, correctly signed,
     * and not expired. Returns false for any other case
     * (tampered, expired, malformed, wrong signature, etc.)
     *
     * This is what our JwtAuthFilter will call on every incoming
     * request to decide whether to treat it as "logged in."
     */
    public boolean isTokenValid(String token) {

        try {
            extractClaims(token);
            return true;

        } catch (Exception e) {
            return false;
        }
    }
}