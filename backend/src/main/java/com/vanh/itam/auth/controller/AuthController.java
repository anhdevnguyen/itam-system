package com.vanh.itam.auth.controller;

import com.vanh.itam.auth.dto.request.LoginRequest;
import com.vanh.itam.auth.dto.response.LoginResponse;
import com.vanh.itam.auth.dto.response.TokenResponse;
import com.vanh.itam.auth.service.AuthService;
import com.vanh.itam.auth.service.AuthServiceImpl;
import com.vanh.itam.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * REST Controller cho Authentication endpoints:
 *   POST /api/v1/auth/login   — đăng nhập, nhận Access Token + set Refresh Token cookie
 *   POST /api/v1/auth/refresh — cấp Access Token mới từ Refresh Token cookie
 *   POST /api/v1/auth/logout  — revoke Refresh Token + xóa cookie
 * Tất cả endpoint đều permit mà không cần xác thực (cấu hình ở SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Đăng nhập, làm mới token, đăng xuất")
public class AuthController {

    private final AuthService authService;
    private final AuthServiceImpl authServiceImpl; // để gọi getAndClearRefreshToken()

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    // ── POST /api/v1/auth/login ───────────────────────────────────────────────

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập", description = "Xác thực email/password, trả Access Token và set Refresh Token httpOnly cookie")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletResponse response) {

        LoginResponse loginResponse = authService.login(request, userAgent);

        // Lấy Refresh Token plaintext vừa sinh (lưu tạm trong ThreadLocal bởi AuthServiceImpl)
        String refreshToken = authServiceImpl.getAndClearRefreshToken();

        // Set Refresh Token vào httpOnly cookie
        setRefreshTokenCookie(response, refreshToken, (int) (refreshTokenExpirationMs / 1000));

        return ResponseEntity.ok(ApiResponse.success(loginResponse));
    }

    // ── POST /api/v1/auth/refresh ─────────────────────────────────────────────

    @PostMapping("/refresh")
    @Operation(summary = "Làm mới Access Token", description = "Đọc Refresh Token từ httpOnly cookie và cấp Access Token mới")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(HttpServletRequest request) {
        String refreshTokenValue = extractRefreshTokenFromCookie(request);

        TokenResponse tokenResponse = authService.refresh(refreshTokenValue);
        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }

    // ── POST /api/v1/auth/logout ──────────────────────────────────────────────

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất", description = "Revoke Refresh Token và xóa cookie. Không yêu cầu Access Token.")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshTokenValue = extractRefreshTokenFromCookie(request);

        // Revoke token trong DB (null-safe — nếu không có cookie vẫn trả 200)
        authService.logout(refreshTokenValue);

        // Xóa cookie khỏi browser bằng cách set Max-Age = 0
        clearRefreshTokenCookie(response);

        return ResponseEntity.ok(ApiResponse.success());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Set Refresh Token vào httpOnly, Secure, SameSite=Lax cookie.
     * Các attribute này bảo vệ chống XSS và giảm thiểu CSRF.
     */
    private void setRefreshTokenCookie(HttpServletResponse response, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, value);
        cookie.setHttpOnly(true);           // Không thể đọc bằng JavaScript
        cookie.setSecure(true);             // Chỉ gửi qua HTTPS
        cookie.setPath("/api/v1/auth");     // Chỉ gửi tới /api/v1/auth/* (giảm exposure)
        cookie.setMaxAge(maxAgeSeconds);

        // SameSite=Lax: cân bằng bảo mật CSRF và trải nghiệm cross-site redirect
        // Spring Boot 3.x Cookie chưa có setters cho SameSite — dùng header thủ công
        response.addCookie(cookie);

        // Override cookie header để thêm SameSite=Lax
        String cookieHeader = String.format(
                "%s=%s; Max-Age=%d; Path=/api/v1/auth; HttpOnly; Secure; SameSite=Lax",
                REFRESH_TOKEN_COOKIE, value, maxAgeSeconds);
        response.addHeader("Set-Cookie", cookieHeader);
    }

    /**
     * Xóa Refresh Token cookie bằng cách set Max-Age = 0 và value rỗng.
     */
    private void clearRefreshTokenCookie(HttpServletResponse response) {
        String cookieHeader = String.format(
                "%s=; Max-Age=0; Path=/api/v1/auth; HttpOnly; Secure; SameSite=Lax",
                REFRESH_TOKEN_COOKIE);
        response.addHeader("Set-Cookie", cookieHeader);
    }

    /**
     * Đọc giá trị Refresh Token từ cookie trong request.
     * Trả null nếu không tìm thấy cookie.
     */
    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        return Arrays.stream(cookies)
                .filter(c -> REFRESH_TOKEN_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
