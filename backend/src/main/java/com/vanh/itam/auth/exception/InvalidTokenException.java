package com.vanh.itam.auth.exception;

import com.vanh.itam.common.exception.UnauthorizedException;

/**
 * Refresh token không hợp lệ — không tìm thấy trong DB, đã revoke, hoặc đã hết hạn.
 * HTTP 401 Unauthorized.
 */
public class InvalidTokenException extends UnauthorizedException {

    public InvalidTokenException() {
        super("AUTH_REFRESH_TOKEN_INVALID", "Refresh token không hợp lệ hoặc đã hết hạn");
    }

    public InvalidTokenException(String errorCode, String message) {
        super(errorCode, message);
    }
}
