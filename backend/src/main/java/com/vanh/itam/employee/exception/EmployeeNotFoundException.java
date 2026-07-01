package com.vanh.itam.employee.exception;

import com.vanh.itam.common.exception.ResourceNotFoundException;

public class EmployeeNotFoundException extends ResourceNotFoundException {

    public EmployeeNotFoundException(Long id) {
        super("EMPLOYEE_NOT_FOUND", "Không tìm thấy nhân viên với ID: " + id);
    }
}
