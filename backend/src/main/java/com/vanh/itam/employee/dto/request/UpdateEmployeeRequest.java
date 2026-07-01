package com.vanh.itam.employee.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateEmployeeRequest {

    @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
    private String fullName;

    private Long roleId;
    private Long branchId;
    private Long departmentId;
}
