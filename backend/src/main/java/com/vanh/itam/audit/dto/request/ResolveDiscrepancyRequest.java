package com.vanh.itam.audit.dto.request;

import com.vanh.itam.audit.entity.ResolutionAction;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResolveDiscrepancyRequest {

    @NotNull(message = "Hành động xử lý là bắt buộc (CONFIRM_LOST hoặc FOUND)")
    private ResolutionAction action;
}
