package com.vanh.itam.request.dto.request;

import com.vanh.itam.request.entity.RequestType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRequestRequest {

    @NotNull(message = "Loại yêu cầu là bắt buộc (ASSIGN hoặc RETURN)")
    private RequestType type;

    @NotNull(message = "Thiết bị là bắt buộc")
    private Long assetId;

    private String note;
}
