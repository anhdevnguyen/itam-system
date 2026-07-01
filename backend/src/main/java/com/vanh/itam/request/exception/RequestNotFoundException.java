package com.vanh.itam.request.exception;

import com.vanh.itam.common.exception.ResourceNotFoundException;

public class RequestNotFoundException extends ResourceNotFoundException {

    public RequestNotFoundException(Long id) {
        super("REQUEST_NOT_FOUND", "Không tìm thấy yêu cầu với ID: " + id);
    }
}
