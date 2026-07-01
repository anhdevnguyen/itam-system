package com.vanh.itam.audit.exception;

import com.vanh.itam.common.exception.BusinessException;

public class AuditSessionAlreadyCompletedException extends BusinessException {

    public AuditSessionAlreadyCompletedException() {
        super("AUDIT_SESSION_ALREADY_COMPLETED", "Phiên kiểm kê đã hoàn tất trước đó");
    }
}
