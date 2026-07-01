package com.vanh.itam.audit.controller;

import com.vanh.itam.audit.dto.request.CreateAuditSessionRequest;
import com.vanh.itam.audit.dto.request.ResolveDiscrepancyRequest;
import com.vanh.itam.audit.dto.request.ScanRequest;
import com.vanh.itam.audit.dto.response.AuditScanResponse;
import com.vanh.itam.audit.dto.response.AuditSessionResponse;
import com.vanh.itam.audit.dto.response.DiscrepancyResponse;
import com.vanh.itam.audit.entity.DiscrepancyStatus;
import com.vanh.itam.audit.service.AuditService;
import com.vanh.itam.common.config.CustomUserDetails;
import com.vanh.itam.common.response.ApiResponse;
import com.vanh.itam.common.response.Pagination;
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
@RequestMapping("/api/v1/audits")
@RequiredArgsConstructor
@Tag(name = "Audits", description = "Kiểm kê thiết bị định kỳ")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AuditSessionResponse>>> getAllSessions(
            @RequestParam(required = false) Long branchId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<AuditSessionResponse> page = auditService.getAllSessions(branchId, pageable);
        return ResponseEntity.ok(ApiResponse.success(page.getContent(),
                Pagination.of(page.getNumber(), page.getSize(),
                        page.getTotalElements(), page.getTotalPages())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AuditSessionResponse>> getSession(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(auditService.getSessionById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF')")
    @Operation(summary = "Tạo phiên kiểm kê mới (expires_at = now + 3 ngày)")
    public ResponseEntity<ApiResponse<AuditSessionResponse>> createSession(
            @Valid @RequestBody CreateAuditSessionRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.status(201).body(ApiResponse.success(
                auditService.createSession(request, currentUser.getEmployeeId())));
    }

    @PostMapping("/{id}/scan")
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF')")
    @Operation(summary = "Ghi nhận lượt quét QR (kiểm tra branch, tạo audit_scan record)")
    public ResponseEntity<ApiResponse<AuditScanResponse>> scan(
            @PathVariable Long id,
            @Valid @RequestBody ScanRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                auditService.scan(id, request, currentUser.getEmployeeId())));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF')")
    @Operation(summary = "Hoàn tất phiên kiểm kê — tự tạo discrepancy MISSING cho asset chưa quét")
    public ResponseEntity<ApiResponse<AuditSessionResponse>> complete(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(auditService.complete(id)));
    }

    @GetMapping("/{id}/discrepancies")
    public ResponseEntity<ApiResponse<List<DiscrepancyResponse>>> getDiscrepancies(
            @PathVariable Long id,
            @RequestParam(required = false) DiscrepancyStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<DiscrepancyResponse> page = auditService.getDiscrepancies(id, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(page.getContent(),
                Pagination.of(page.getNumber(), page.getSize(),
                        page.getTotalElements(), page.getTotalPages())));
    }

    @PostMapping("/discrepancies/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF')")
    @Operation(summary = "Xử lý sai lệch (CONFIRM_LOST → asset LOST, FOUND → giữ nguyên)")
    public ResponseEntity<ApiResponse<DiscrepancyResponse>> resolveDiscrepancy(
            @PathVariable Long id,
            @Valid @RequestBody ResolveDiscrepancyRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                auditService.resolveDiscrepancy(id, request, currentUser.getEmployeeId())));
    }
}
