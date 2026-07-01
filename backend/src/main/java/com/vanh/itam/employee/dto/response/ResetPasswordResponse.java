package com.vanh.itam.employee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResetPasswordResponse {
    private String temporaryPassword;
}
