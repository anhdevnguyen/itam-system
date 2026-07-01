package com.vanh.itam.maintenance.dto.request;

import com.vanh.itam.maintenance.entity.MaintenanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateMaintenanceRequest {

    @NotNull(message = "Trạng thái là bắt buộc")
    private MaintenanceStatus status;

    private String description;

    private LocalDate scheduledDate;

    private LocalDate completedDate;
}
