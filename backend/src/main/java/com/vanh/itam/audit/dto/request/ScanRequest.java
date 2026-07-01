package com.vanh.itam.audit.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScanRequest {

    @NotBlank(message = "Mã thiết bị là bắt buộc")
    private String assetCode;

    private String scannedLocation;
}
