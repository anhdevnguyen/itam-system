package com.vanh.itam.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Abstract base cho mọi exception của hệ thống ITAM.
 * Subclass chỉ cần gọi super(errorCode, httpStatus, message).
 */
@Getter
public abstract class BaseException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    protected BaseException(String errorCode, HttpStatus httpStatus, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
