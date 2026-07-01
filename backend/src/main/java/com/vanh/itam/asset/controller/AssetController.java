package com.vanh.itam.asset.controller;

import com.vanh.itam.asset.dto.request.CreateAssetRequest;
import com.vanh.itam.asset.dto.request.ForceReturnRequest;
import com.vanh.itam.asset.dto.request.UpdateAssetRequest;
import com.vanh.itam.asset.dto.response.AssetAssignmentHistoryResponse;
import com.vanh.itam.asset.dto.response.AssetImageResponse;
import com.vanh.itam.asset.dto.response.AssetResponse;
import com.vanh.itam.asset.entity.AssetStatus;
import com.vanh.itam.asset.service.AssetService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
@Tag(name = "Assets", description = "Quản lý thiết bị IT")
public class AssetController {

    private final AssetService assetService;

    @GetMapping
    @Operation(summary = "Danh sách thiết bị (pagination, filter)")
    public ResponseEntity<ApiResponse<List<AssetResponse>>> getAll(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long assignedToId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<AssetResponse> page = assetService.getAll(branchId, status, categoryId, assignedToId, pageable);
        return ResponseEntity.ok(ApiResponse.success(page.getContent(),
                Pagination.of(page.getNumber(), page.getSize(),
                        page.getTotalElements(), page.getTotalPages())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết thiết bị")
    public ResponseEntity<ApiResponse<AssetResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(assetService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF')")
    @Operation(summary = "Tạo thiết bị mới (code tự sinh)")
    public ResponseEntity<ApiResponse<AssetResponse>> create(@Valid @RequestBody CreateAssetRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success(assetService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF')")
    @Operation(summary = "Cập nhật thiết bị")
    public ResponseEntity<ApiResponse<AssetResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAssetRequest request) {
        return ResponseEntity.ok(ApiResponse.success(assetService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF')")
    @Operation(summary = "Soft-delete thiết bị")
    public ResponseEntity<ApiResponse<Void>> softDelete(@PathVariable Long id) {
        assetService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF')")
    @Operation(summary = "Khôi phục thiết bị đã xóa")
    public ResponseEntity<ApiResponse<AssetResponse>> restore(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(assetService.restore(id)));
    }

    @GetMapping("/{id}/qr-code")
    @Operation(summary = "Lấy QR code PNG của thiết bị")
    public ResponseEntity<byte[]> getQrCode(@PathVariable Long id) {
        byte[] qrBytes = assetService.getQrCode(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                .body(qrBytes);
    }

    @GetMapping("/{id}/assignment-history")
    @Operation(summary = "Lịch sử cấp phát của thiết bị")
    public ResponseEntity<ApiResponse<List<AssetAssignmentHistoryResponse>>> getHistory(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AssetAssignmentHistoryResponse> page = assetService.getAssignmentHistory(id, pageable);
        return ResponseEntity.ok(ApiResponse.success(page.getContent(),
                Pagination.of(page.getNumber(), page.getSize(),
                        page.getTotalElements(), page.getTotalPages())));
    }

    @PostMapping("/{id}/force-return")
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF')")
    @Operation(summary = "IT Staff chủ động thu hồi thiết bị (bỏ qua workflow)")
    public ResponseEntity<ApiResponse<AssetResponse>> forceReturn(
            @PathVariable Long id,
            @Valid @RequestBody ForceReturnRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                assetService.forceReturn(id, request, currentUser.getEmployeeId())));
    }

    @PostMapping("/{id}/images")
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF')")
    @Operation(summary = "Upload ảnh thiết bị (max 5 ảnh, mỗi ảnh ≤5MB)")
    public ResponseEntity<ApiResponse<AssetImageResponse>> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(201).body(ApiResponse.success(assetService.uploadImage(id, file)));
    }

    @GetMapping("/{id}/images")
    @Operation(summary = "Danh sách ảnh của thiết bị")
    public ResponseEntity<ApiResponse<List<AssetImageResponse>>> getImages(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(assetService.getImages(id)));
    }
}
