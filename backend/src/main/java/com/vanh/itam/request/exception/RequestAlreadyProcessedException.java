package com.vanh.itam.request.exception;

import com.vanh.itam.common.exception.BusinessException;

public class RequestAlreadyProcessedException extends BusinessException {

    public RequestAlreadyProcessedException() {
        super("REQUEST_ALREADY_PROCESSED", "Yêu cầu đã được xử lý trước đó");
    }
}
