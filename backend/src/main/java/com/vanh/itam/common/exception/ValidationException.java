package com.vanh.itam.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Lỗi validation nghiệp vụ phức tạp (ngoài Jakarta Bean Validation annotation).
 * HTTP 400 Bad Request.
 */
public class ValidationException extends BaseException {

    public ValidationException(String errorCode, String message) {
        super(errorCode, HttpStatus.BAD_REQUEST, message);
    }
}
