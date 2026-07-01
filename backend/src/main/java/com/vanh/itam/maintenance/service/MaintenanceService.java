package com.vanh.itam.maintenance.service;

import com.vanh.itam.maintenance.dto.request.CreateMaintenanceRequest;
import com.vanh.itam.maintenance.dto.request.UpdateMaintenanceRequest;
import com.vanh.itam.maintenance.dto.response.MaintenanceResponse;
import com.vanh.itam.maintenance.entity.MaintenanceStatus;
import com.vanh.itam.maintenance.entity.MaintenanceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MaintenanceService {
    Page<MaintenanceResponse> getAll(Long assetId, MaintenanceStatus status, MaintenanceType type, Pageable pageable);
    MaintenanceResponse getById(Long id);
    MaintenanceResponse create(CreateMaintenanceRequest request);
    MaintenanceResponse update(Long id, UpdateMaintenanceRequest request);
    void softDelete(Long id);
}
