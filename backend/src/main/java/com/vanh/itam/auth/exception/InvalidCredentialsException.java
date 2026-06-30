package com.vanh.itam.auth.exception;

import com.vanh.itam.common.exception.UnauthorizedException;

/**
 * Sai email hoặc sai mật khẩu khi đăng nhập.
 * Message gộp chung 2 trường hợp để chống user enumeration
 * (không phân biệt "email không tồn tại" vs "sai mật khẩu").
 * Xem docs/06-AUTHENTICATION.md mục 9.
 */
public class InvalidCredentialsException extends UnauthorizedException {

    public InvalidCredentialsException() {
        super("AUTH_INVALID_CREDENTIALS", "Email hoặc mật khẩu không đúng");
    }
}
