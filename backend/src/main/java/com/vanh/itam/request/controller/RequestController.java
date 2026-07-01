package com.vanh.itam.request.controller;

import com.vanh.itam.common.config.CustomUserDetails;
import com.vanh.itam.common.response.ApiResponse;
import com.vanh.itam.common.response.Pagination;
import com.vanh.itam.request.dto.request.CreateRequestRequest;
import com.vanh.itam.request.dto.request.RejectRequestRequest;
import com.vanh.itam.request.dto.response.RequestResponse;
import com.vanh.itam.request.entity.RequestStatus;
import com.vanh.itam.request.service.RequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
@Tag(name = "Requests", description = "Yêu cầu cấp phát / thu hồi thiết bị")
public class RequestController {

    private final RequestService requestService;

    @GetMapping
    @Operation(summary = "Danh sách yêu cầu")
    public ResponseEntity<ApiResponse<List<RequestResponse>>> getAll(
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long branchId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<RequestResponse> page = requestService.getAll(status, employeeId, branchId, pageable);
        return ResponseEntity.ok(ApiResponse.success(page.getContent(),
                Pagination.of(page.getNumber(), page.getSize(),
                        page.getTotalElements(), page.getTotalPages())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RequestResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(requestService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Employee tạo yêu cầu ASSIGN hoặc RETURN")
    public ResponseEntity<ApiResponse<RequestResponse>> create(
            @Valid @RequestBody CreateRequestRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.status(201).body(ApiResponse.success(
                requestService.create(request, currentUser.getEmployeeId())));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Manager duyệt yêu cầu")
    public ResponseEntity<ApiResponse<RequestResponse>> approve(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                requestService.approve(id, currentUser.getEmployeeId())));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Manager từ chối yêu cầu (bắt buộc nhập lý do)")
    public ResponseEntity<ApiResponse<RequestResponse>> reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectRequestRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                requestService.reject(id, request, currentUser.getEmployeeId())));
    }

    @PostMapping("/{id}/fulfill")
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF')")
    @Operation(summary = "IT Staff hoàn tất cấp phát/thu hồi")
    public ResponseEntity<ApiResponse<RequestResponse>> fulfill(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                requestService.fulfill(id, currentUser.getEmployeeId())));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Employee hủy yêu cầu (chỉ khi PENDING)")
    public ResponseEntity<ApiResponse<RequestResponse>> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                requestService.cancel(id, currentUser.getEmployeeId())));
    }
}
