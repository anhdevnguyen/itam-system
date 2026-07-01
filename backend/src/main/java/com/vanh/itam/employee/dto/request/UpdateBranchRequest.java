package com.vanh.itam.employee.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBranchRequest {

    @NotBlank(message = "Tên chi nhánh không được để trống")
    @Size(max = 100, message = "Tên chi nhánh tối đa 100 ký tự")
    private String name;

    @Size(max = 255)
    private String address;
}
