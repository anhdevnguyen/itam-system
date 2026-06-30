package com.vanh.itam.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class sinh và validate JWT Access Token.
 * Dùng JJWT 0.12.6, thuật toán HS256.
 * Claims payload:
 *   sub      = employee.id (String)
 *   email    = employee.email
 *   role     = role.code (VD: IT_STAFF)
 *   branchId = employee.branch.id (Long)
 */
@Component
@Slf4j
public class JwtUtil {

    private final SecretKey signingKey;
    private final long accessTokenExpirationMs;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpirationMs) {
        // Key phải đủ 256-bit (32 byte) cho HS256
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    /**
     * Sinh Access Token JWT với các claims: sub, email, role, branchId.
     */
    public String generateAccessToken(Long employeeId, String email, String roleCode, Long branchId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("role", roleCode);
        claims.put("branchId", branchId);

        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(employeeId))
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessTokenExpirationMs))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validate token và trả về Claims.
     * Ném JwtException nếu token không hợp lệ hoặc đã hết hạn.
     */
    public Claims validateAndGetClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Lấy employee ID từ token (claim "sub").
     * @throws JwtException nếu token không hợp lệ
     */
    public Long extractEmployeeId(String token) {
        return Long.valueOf(validateAndGetClaims(token).getSubject());
    }

    /**
     * Kiểm tra token có hợp lệ không (signature + expiry).
     */
    public boolean isTokenValid(String token) {
        try {
            validateAndGetClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }
}
