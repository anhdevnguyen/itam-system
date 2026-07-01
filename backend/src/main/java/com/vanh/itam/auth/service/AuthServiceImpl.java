package com.vanh.itam.auth.service;

import com.vanh.itam.auth.dto.request.LoginRequest;
import com.vanh.itam.auth.dto.response.LoginResponse;
import com.vanh.itam.auth.dto.response.TokenResponse;
import com.vanh.itam.auth.entity.RefreshToken;
import com.vanh.itam.auth.exception.InvalidCredentialsException;
import com.vanh.itam.auth.exception.InvalidTokenException;
import com.vanh.itam.auth.mapper.AuthMapper;
import com.vanh.itam.auth.repository.RefreshTokenRepository;
import com.vanh.itam.employee.entity.Employee;
import com.vanh.itam.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import com.vanh.itam.common.util.JwtUtil;

/**
 * Implementation logic xác thực cho ITAM.
 * Login:
 *   1. Tìm employee theo email (chỉ lấy chưa soft-delete)
 *   2. Verify BCrypt password — nếu sai → InvalidCredentialsException (gộp chung 2 lỗi chống user enumeration)
 *   3. Sinh Access Token JWT (30 phút)
 *   4. Sinh Refresh Token opaque (UUID random) + lưu SHA-256 hash vào DB
 *   5. Trả LoginResponse + refresh token plaintext (Controller set cookie)
 * Refresh:
 *   1. Đọc Refresh Token plaintext từ cookie
 *   2. Hash SHA-256 → tra cứu DB
 *   3. Kiểm tra expires_at
 *   4. Sinh Access Token mới — Refresh Token GIỮ NGUYÊN (không rotate ở MVP)
 * Logout:
 *   1. Hash SHA-256 của Refresh Token plaintext
 *   2. Hard delete record khỏi DB (xóa đúng 1 record — thiết bị hiện tại)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final EmployeeRepository employeeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthMapper authMapper;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    // ThreadLocal lưu tạm refresh token plaintext để Controller lấy set cookie
    private final ThreadLocal<String> refreshTokenHolder = new ThreadLocal<>();

    // ── Login ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String userAgent) {
        // 1. Tìm employee theo email — chỉ lấy chưa bị soft-delete
        Employee employee = employeeRepository
                .findByEmailAndNotDeleted(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        // 2. Verify mật khẩu BCrypt
        // Gộp chung lỗi "email không tồn tại" và "sai mật khẩu" → tránh user enumeration
        if (!passwordEncoder.matches(request.getPassword(), employee.getPasswordHash())) {
            log.warn("Login failed — wrong password: email={}", request.getEmail());
            throw new InvalidCredentialsException();
        }

        // 3. Sinh Access Token
        String accessToken = jwtUtil.generateAccessToken(
                employee.getId(),
                employee.getEmail(),
                employee.getRole().getCode(),
                employee.getBranch().getId()
        );

        // 4. Sinh Refresh Token opaque + lưu hash vào DB
        String refreshTokenValue = UUID.randomUUID().toString();
        String tokenHash = sha256(refreshTokenValue);
        String deviceLabel = abbreviateUserAgent(userAgent);
        Instant expiresAt = Instant.now().plusMillis(refreshTokenExpirationMs);

        RefreshToken refreshToken = RefreshToken.create(employee, tokenHash, deviceLabel, expiresAt);
        refreshTokenRepository.save(refreshToken);

        // 5. Lưu tạm plaintext để Controller lấy set vào cookie
        refreshTokenHolder.set(refreshTokenValue);

        long expiresInSeconds = jwtUtil.getAccessTokenExpirationMs() / 1000;
        LoginResponse.UserInfo userInfo = authMapper.toUserInfo(employee);

        log.info("Login successful: employeeId={}, email={}, role={}, device={}",
                employee.getId(), employee.getEmail(), employee.getRole().getCode(), deviceLabel);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .expiresIn(expiresInSeconds)
                .user(userInfo)
                .build();
    }

    /**
     * Lấy Refresh Token plaintext vừa sinh — PHẢI gọi ngay sau login().
     * ThreadLocal được clear sau khi lấy.
     */
    public String getAndClearRefreshToken() {
        String token = refreshTokenHolder.get();
        refreshTokenHolder.remove();
        return token;
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TokenResponse refresh(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new InvalidTokenException();
        }

        // Tra cứu DB theo hash
        String tokenHash = sha256(refreshTokenValue);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(InvalidTokenException::new);

        // Kiểm tra hết hạn
        if (stored.isExpired()) {
            // Dọn dẹp token đã hết hạn
            refreshTokenRepository.delete(stored);
            log.warn("Refresh token expired: employeeId={}", stored.getEmployee().getId());
            throw new InvalidTokenException();
        }

        // Sinh Access Token mới
        Employee employee = stored.getEmployee();
        String newAccessToken = jwtUtil.generateAccessToken(
                employee.getId(),
                employee.getEmail(),
                employee.getRole().getCode(),
                employee.getBranch().getId()
        );

        long expiresInSeconds = jwtUtil.getAccessTokenExpirationMs() / 1000;

        log.info("Token refreshed: employeeId={}, email={}", employee.getId(), employee.getEmail());

        return new TokenResponse(newAccessToken, expiresInSeconds);
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void logout(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            // Không có cookie — graceful no-op, không throw exception
            log.debug("Logout called with no refresh token — no-op");
            return;
        }

        String tokenHash = sha256(refreshTokenValue);
        refreshTokenRepository.deleteByTokenHash(tokenHash);

        log.info("Logout successful: tokenHash prefix={}", tokenHash.substring(0, 8));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Tính SHA-256 hex của chuỗi đầu vào.
     * Dùng để hash Refresh Token trước khi lưu/tra cứu DB.
     */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 luôn có sẵn trong JVM chuẩn — không thể xảy ra
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Rút gọn User-Agent thành label ngắn gọn cho device_label.
     * VD: "Chrome on Windows", "Safari on iPhone".
     */
    private String abbreviateUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown Device";
        }
        // Rút gọn: lấy tối đa 200 ký tự đầu để tránh overflow cột VARCHAR(255)
        String ua = userAgent.length() > 200 ? userAgent.substring(0, 200) : userAgent;

        // Nhận diện trình duyệt phổ biến
        if (ua.contains("Edg/")) return "Edge on " + detectOS(ua);
        if (ua.contains("Chrome/")) return "Chrome on " + detectOS(ua);
        if (ua.contains("Firefox/")) return "Firefox on " + detectOS(ua);
        if (ua.contains("Safari/") && !ua.contains("Chrome")) return "Safari on " + detectOS(ua);
        if (ua.contains("Postman")) return "Postman";
        return "Browser on " + detectOS(ua);
    }

    private String detectOS(String ua) {
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("iPhone") || ua.contains("iPad")) return "iOS";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("Macintosh") || ua.contains("Mac OS")) return "macOS";
        if (ua.contains("Linux")) return "Linux";
        return "Unknown OS";
    }
}
