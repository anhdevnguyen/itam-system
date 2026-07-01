package com.vanh.itam.auth.service;

import com.vanh.itam.auth.dto.request.LoginRequest;
import com.vanh.itam.auth.dto.response.LoginResponse;
import com.vanh.itam.auth.dto.response.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Interface cho auth business logic.
 */
public interface AuthService {

    /**
     * Xác thực email/password, sinh Access Token + Refresh Token.
     *
     * @param request  LoginRequest với email và password
     * @param userAgent User-Agent header — dùng làm device_label
     * @return LoginResponse gồm accessToken, expiresIn, user info
     *         Refresh Token được trả riêng (caller set vào httpOnly cookie)
     */
    LoginResponse login(LoginRequest request, String userAgent);

    /**
     * Lấy giá trị Refresh Token plaintext vừa sinh trong lần login.
     * Dùng nội bộ để set vào cookie sau khi login().
     * Được lưu tạm trong ThreadLocal hoặc trả qua object wrapper.
     * → Thực tế: login() trả về LoginResponse và Controller tự xử lý cookie;
     *   ta dùng phương án trả Refresh Token trực tiếp qua LoginResult record.
     * Xác thực Refresh Token từ cookie, cấp Access Token mới.
     *
     * @param refreshTokenValue giá trị plaintext từ cookie
     * @return TokenResponse gồm accessToken mới và expiresIn
     */
    TokenResponse refresh(String refreshTokenValue);

    /**
     * Revoke Refresh Token (hard delete khỏi DB) và xóa cookie.
     *
     * @param refreshTokenValue giá trị plaintext từ cookie (null-safe — nếu null thì bỏ qua)
     */
    void logout(String refreshTokenValue);
}
