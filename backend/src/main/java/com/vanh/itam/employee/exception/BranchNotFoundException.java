package com.vanh.itam.employee.exception;

import com.vanh.itam.common.exception.ResourceNotFoundException;

public class BranchNotFoundException extends ResourceNotFoundException {

    public BranchNotFoundException(Long id) {
        super("BRANCH_NOT_FOUND", "Không tìm thấy chi nhánh với ID: " + id);
    }
}
