package com.vanh.itam.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Lỗi nghiệp vụ có chủ đích (VD: asset không khả dụng, request đã xử lý).
 * Mặc định HTTP 409 Conflict — override constructor nếu cần status khác.
 * Log ở mức WARN (không phải ERROR).
 */
public class BusinessException extends BaseException {

    public BusinessException(String errorCode, String message) {
        super(errorCode, HttpStatus.CONFLICT, message);
    }

    public BusinessException(String errorCode, HttpStatus httpStatus, String message) {
        super(errorCode, httpStatus, message);
    }
}
