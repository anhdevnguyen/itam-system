package com.vanh.itam.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Dùng làm base cho mọi XxxNotFoundException (404 Not Found).
 */
public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String errorCode, String message) {
        super(errorCode, HttpStatus.NOT_FOUND, message);
    }
}
