package com.vanh.itam.audit.exception;

import com.vanh.itam.common.exception.BusinessException;

public class DiscrepancyAlreadyResolvedException extends BusinessException {

    public DiscrepancyAlreadyResolvedException() {
        super("DISCREPANCY_ALREADY_RESOLVED", "Sai lệch này đã được xử lý trước đó");
    }
}
