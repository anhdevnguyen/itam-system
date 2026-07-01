package com.vanh.itam.maintenance.dto.request;

import com.vanh.itam.maintenance.entity.MaintenanceType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateMaintenanceRequest {

    @NotNull(message = "Thiết bị là bắt buộc")
    private Long assetId;

    @NotNull(message = "Loại bảo trì là bắt buộc")
    private MaintenanceType type;

    private String description;

    private LocalDate scheduledDate;
}
