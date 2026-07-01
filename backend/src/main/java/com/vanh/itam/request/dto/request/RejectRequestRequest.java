package com.vanh.itam.request.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectRequestRequest {

    @NotBlank(message = "Lý do từ chối là bắt buộc")
    private String rejectionReason;
}
