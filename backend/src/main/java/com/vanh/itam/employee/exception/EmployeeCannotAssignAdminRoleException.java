package com.vanh.itam.employee.exception;

import com.vanh.itam.common.exception.ForbiddenException;

public class EmployeeCannotAssignAdminRoleException extends ForbiddenException {

    public EmployeeCannotAssignAdminRoleException() {
        super("EMPLOYEE_CANNOT_ASSIGN_ADMIN_ROLE",
                "IT Staff không được phép tạo hoặc sửa tài khoản có vai trò ADMIN");
    }
}
