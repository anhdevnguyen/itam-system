package com.vanh.itam.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Response body cho POST /api/v1/auth/login
 * Khớp spec docs/04-API.md mục 4:
 * { accessToken, expiresIn, user: { id, email, fullName, role, branchId, mustChangePassword } }
 * Refresh Token KHÔNG có trong body — được set qua HttpOnly cookie.
 */
@Getter
@Builder
public class LoginResponse {

    private String accessToken;

    /** Thời gian sống Access Token tính bằng giây (thường 1800 = 30 phút) */
    private long expiresIn;

    private UserInfo user;

    @Getter
    @Builder
    public static class UserInfo {
        private Long id;
        private String email;
        private String fullName;
        private String role;
        private Long branchId;
        private boolean mustChangePassword;
    }
}
