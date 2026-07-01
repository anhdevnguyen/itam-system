package com.vanh.itam.asset.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForceReturnRequest {

    @NotBlank(message = "Lý do thu hồi là bắt buộc")
    private String reason;
}
