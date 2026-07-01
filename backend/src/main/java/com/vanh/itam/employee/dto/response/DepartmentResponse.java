package com.vanh.itam.employee.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class DepartmentResponse {
    private Long id;
    private String name;
    private Long branchId;
    private String branchName;
    private Long managerId;
    private String managerName;
    private Instant createdAt;
}
