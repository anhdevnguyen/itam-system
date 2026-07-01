package com.vanh.itam.employee.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class EmployeeResponse {
    private Long id;
    private String email;
    private String fullName;
    private Long roleId;
    private String roleCode;
    private Long branchId;
    private String branchName;
    private Long departmentId;
    private String departmentName;
    private boolean mustChangePassword;
    private Instant createdAt;
    private Instant updatedAt;
}
