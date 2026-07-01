package com.vanh.itam.request.entity;

public enum RequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    FULFILLED,
    CANCELLED;

    public boolean isTerminal() {
        return this == REJECTED || this == FULFILLED || this == CANCELLED;
    }
}
