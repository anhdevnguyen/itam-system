package com.vanh.itam.audit.exception;

import com.vanh.itam.common.exception.ResourceNotFoundException;

public class DiscrepancyNotFoundException extends ResourceNotFoundException {

    public DiscrepancyNotFoundException(Long id) {
        super("DISCREPANCY_NOT_FOUND", "Không tìm thấy sai lệch với ID: " + id);
    }
}
