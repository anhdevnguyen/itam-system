package com.vanh.itam.employee.controller;

import com.vanh.itam.common.response.ApiResponse;
import com.vanh.itam.employee.dto.request.CreateBranchRequest;
import com.vanh.itam.employee.dto.request.UpdateBranchRequest;
import com.vanh.itam.employee.dto.response.BranchResponse;
import com.vanh.itam.employee.service.BranchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
@Tag(name = "Branches", description = "Quản lý chi nhánh")
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    @Operation(summary = "Lấy danh sách chi nhánh")
    public ResponseEntity<ApiResponse<List<BranchResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(branchService.getAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết chi nhánh")
    public ResponseEntity<ApiResponse<BranchResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(branchService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo chi nhánh mới (chỉ Admin)")
    public ResponseEntity<ApiResponse<BranchResponse>> create(
            @Valid @RequestBody CreateBranchRequest request) {
        BranchResponse response = branchService.create(request);
        return ResponseEntity.status(201).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật chi nhánh (chỉ Admin)")
    public ResponseEntity<ApiResponse<BranchResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBranchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(branchService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-delete chi nhánh (chỉ Admin)")
    public ResponseEntity<ApiResponse<Void>> softDelete(@PathVariable Long id) {
        branchService.softDelete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Khôi phục chi nhánh đã xoá (chỉ Admin)")
    public ResponseEntity<ApiResponse<BranchResponse>> restore(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(branchService.restore(id)));
    }
}
