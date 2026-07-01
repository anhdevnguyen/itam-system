package com.vanh.itam.audit.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAuditSessionRequest {

    @NotNull(message = "Chi nhánh là bắt buộc")
    private Long branchId;

    private String note;
}
