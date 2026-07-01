package com.vanh.itam.maintenance.exception;

import com.vanh.itam.common.exception.ResourceNotFoundException;

public class MaintenanceRecordNotFoundException extends ResourceNotFoundException {

    public MaintenanceRecordNotFoundException(Long id) {
        super("MAINTENANCE_RECORD_NOT_FOUND", "Không tìm thấy bản ghi bảo trì với ID: " + id);
    }
}
