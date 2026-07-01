package com.vanh.itam.maintenance.controller;

import com.vanh.itam.common.response.ApiResponse;
import com.vanh.itam.common.response.Pagination;
import com.vanh.itam.maintenance.dto.request.CreateMaintenanceRequest;
import com.vanh.itam.maintenance.dto.request.UpdateMaintenanceRequest;
import com.vanh.itam.maintenance.dto.response.MaintenanceResponse;
import com.vanh.itam.maintenance.entity.MaintenanceStatus;
import com.vanh.itam.maintenance.entity.MaintenanceType;
import com.vanh.itam.maintenance.service.MaintenanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/maintenance")
@RequiredArgsConstructor
@Tag(name = "Maintenance", description = "Bảo hành / bảo trì thiết bị")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MaintenanceResponse>>> getAll(
            @RequestParam(required = false) Long assetId,
            @RequestParam(required = false) MaintenanceStatus status,
            @RequestParam(required = false) MaintenanceType type,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<MaintenanceResponse> page = maintenanceService.getAll(assetId, status, type, pageable);
        return ResponseEntity.ok(ApiResponse.success(page.getContent(),
                Pagination.of(page.getNumber(), page.getSize(),
                        page.getTotalElements(), page.getTotalPages())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(maintenanceService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF')")
    @Operation(summary = "Tạo bản ghi bảo trì mới")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> create(
            @Valid @RequestBody CreateMaintenanceRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success(maintenanceService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF')")
    @Operation(summary = "Cập nhật trạng thái bảo trì (đồng bộ asset.status tự động)")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMaintenanceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(maintenanceService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF')")
    public ResponseEntity<ApiResponse<Void>> softDelete(@PathVariable Long id) {
        maintenanceService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
