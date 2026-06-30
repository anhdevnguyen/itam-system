package com.vanh.itam.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Response body cho POST /api/v1/auth/refresh
 * Chỉ trả accessToken mới — Refresh Token không thay đổi (không rotate ở MVP).
 */
@Getter
@AllArgsConstructor
public class TokenResponse {

    private String accessToken;

    /** Thời gian sống Access Token tính bằng giây */
    private long expiresIn;
}
