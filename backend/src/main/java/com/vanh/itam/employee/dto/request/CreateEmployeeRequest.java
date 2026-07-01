package com.vanh.itam.employee.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateEmployeeRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotNull(message = "Vai trò là bắt buộc")
    private Long roleId;

    @NotNull(message = "Chi nhánh là bắt buộc")
    private Long branchId;

    // nullable — Admin/IT Staff trung tâm có thể không gắn department
    private Long departmentId;
}
