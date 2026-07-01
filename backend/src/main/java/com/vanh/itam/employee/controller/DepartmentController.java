package com.vanh.itam.employee.controller;

import com.vanh.itam.common.response.ApiResponse;
import com.vanh.itam.common.response.Pagination;
import com.vanh.itam.employee.dto.request.CreateDepartmentRequest;
import com.vanh.itam.employee.dto.request.UpdateDepartmentRequest;
import com.vanh.itam.employee.dto.response.DepartmentResponse;
import com.vanh.itam.employee.service.DepartmentService;
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
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Tag(name = "Departments", description = "Quản lý phòng ban")
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    @Operation(summary = "Lấy danh sách phòng ban (filter theo branchId)")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> getAll(
            @RequestParam(required = false) Long branchId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<DepartmentResponse> page = departmentService.getAll(branchId, pageable);
        return ResponseEntity.ok(ApiResponse.success(page.getContent(),
                Pagination.of(page.getNumber(), page.getSize(),
                        page.getTotalElements(), page.getTotalPages())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết phòng ban")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(departmentService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF')")
    @Operation(summary = "Tạo phòng ban mới")
    public ResponseEntity<ApiResponse<DepartmentResponse>> create(
            @Valid @RequestBody CreateDepartmentRequest request) {
        DepartmentResponse response = departmentService.create(request);
        return ResponseEntity.status(201).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF')")
    @Operation(summary = "Cập nhật phòng ban (gán/đổi Manager)")
    public ResponseEntity<ApiResponse<DepartmentResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDepartmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(departmentService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF')")
    @Operation(summary = "Soft-delete phòng ban")
    public ResponseEntity<ApiResponse<Void>> softDelete(@PathVariable Long id) {
        departmentService.softDelete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
