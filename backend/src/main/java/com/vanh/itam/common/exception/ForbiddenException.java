package com.vanh.itam.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Đúng role nhưng không đủ quyền (sai scope, must_change_password, ...).
 * HTTP 403 Forbidden.
 */
public class ForbiddenException extends BaseException {

    public ForbiddenException(String errorCode, String message) {
        super(errorCode, HttpStatus.FORBIDDEN, message);
    }
}
