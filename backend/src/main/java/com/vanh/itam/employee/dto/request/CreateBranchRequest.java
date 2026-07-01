package com.vanh.itam.employee.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBranchRequest {

    @NotBlank(message = "Mã chi nhánh không được để trống")
    @Size(max = 10, message = "Mã chi nhánh tối đa 10 ký tự")
    private String code;

    @NotBlank(message = "Tên chi nhánh không được để trống")
    @Size(max = 100, message = "Tên chi nhánh tối đa 100 ký tự")
    private String name;

    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String address;
}
