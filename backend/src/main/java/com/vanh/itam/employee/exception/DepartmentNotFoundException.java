package com.vanh.itam.employee.exception;

import com.vanh.itam.common.exception.ResourceNotFoundException;

public class DepartmentNotFoundException extends ResourceNotFoundException {

    public DepartmentNotFoundException(Long id) {
        super("DEPARTMENT_NOT_FOUND", "Không tìm thấy phòng ban với ID: " + id);
    }
}
