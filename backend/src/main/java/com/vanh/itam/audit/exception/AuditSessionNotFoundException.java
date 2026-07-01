package com.vanh.itam.audit.exception;

import com.vanh.itam.common.exception.ResourceNotFoundException;

public class AuditSessionNotFoundException extends ResourceNotFoundException {

    public AuditSessionNotFoundException(Long id) {
        super("AUDIT_SESSION_NOT_FOUND", "Không tìm thấy phiên kiểm kê với ID: " + id);
    }
}
