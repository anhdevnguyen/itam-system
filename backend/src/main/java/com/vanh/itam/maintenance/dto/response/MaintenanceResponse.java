package com.vanh.itam.maintenance.dto.response;

import com.vanh.itam.maintenance.entity.MaintenanceStatus;
import com.vanh.itam.maintenance.entity.MaintenanceType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Builder
public class MaintenanceResponse {
    private Long id;
    private Long assetId;
    private String assetCode;
    private String assetName;
    private MaintenanceType type;
    private MaintenanceStatus status;
    private String description;
    private LocalDate scheduledDate;
    private LocalDate completedDate;
    private Instant createdAt;
    private Instant updatedAt;
}
