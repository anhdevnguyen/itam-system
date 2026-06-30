package com.vanh.itam.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Chưa đăng nhập hoặc token hết hạn/không hợp lệ. HTTP 401 Unauthorized.
 */
public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String errorCode, String message) {
        super(errorCode, HttpStatus.UNAUTHORIZED, message);
    }
}
