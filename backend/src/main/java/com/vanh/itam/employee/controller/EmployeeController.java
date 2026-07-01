package com.vanh.itam.employee.controller;

import com.vanh.itam.common.config.CustomUserDetails;
import com.vanh.itam.common.response.ApiResponse;
import com.vanh.itam.common.response.Pagination;
import com.vanh.itam.employee.dto.request.ChangePasswordRequest;
import com.vanh.itam.employee.dto.request.CreateEmployeeRequest;
import com.vanh.itam.employee.dto.request.UpdateEmployeeRequest;
import com.vanh.itam.employee.dto.response.EmployeeResponse;
import com.vanh.itam.employee.dto.response.ResetPasswordResponse;
import com.vanh.itam.employee.service.EmployeeService;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Employees", description = "Quản lý nhân viên")
public class EmployeeController {

    // [C2] Inject interface — không inject implementation trực tiếp
    private final EmployeeService employeeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF','MANAGER')")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getAll(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String roleCode,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<EmployeeResponse> page = employeeService.getAll(branchId, departmentId, roleCode, pageable);
        return ResponseEntity.ok(ApiResponse.success(page.getContent(),
                Pagination.of(page.getNumber(), page.getSize(),
                        page.getTotalElements(), page.getTotalPages())));
    }

    @GetMapping("/me")
    @Operation(summary = "Thông tin tài khoản đang đăng nhập")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getMe(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                employeeService.getMe(currentUser.getEmployeeId())));
    }

    @PutMapping("/me")
    @Operation(summary = "Tự cập nhật hồ sơ cá nhân (chỉ fullName)")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateMe(
            @Valid @RequestBody UpdateEmployeeRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                employeeService.updateMe(currentUser.getEmployeeId(), request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF','MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF')")
    @Operation(summary = "Tạo nhân viên mới — mật khẩu tạm tự sinh, mustChangePassword=true")
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(
            @Valid @RequestBody CreateEmployeeRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        EmployeeService.EmployeeCreateResult result =
                employeeService.createWithPassword(request, currentUser.getRoleCode());
        Map<String, Object> data = Map.of(
                "employee", result.employee(),
                "temporaryPassword", result.temporaryPassword());
        return ResponseEntity.status(201).body(ApiResponse.success(data));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                employeeService.update(id, request, currentUser.getRoleCode())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF')")
    @Operation(summary = "Soft-delete nhân viên. Trả 200 + warning nếu đang giữ thiết bị (thay vì 204 — trade-off có chủ ý)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> softDelete(@PathVariable Long id) {
        // Đếm trước khi delete để build warning
        long assetCount = employeeService.countAssignedAssets(id);
        EmployeeResponse deleted = employeeService.softDelete(id);

        // [M7] Dùng Instant.now() vì deletedAt được set trong softDelete() ngay lúc này
        Instant deletedAt = Instant.now();

        if (assetCount > 0) {
            Map<String, Object> data = Map.of(
                    "id", id,
                    "deletedAt", deletedAt,
                    "warning", Map.of(
                            "code", "EMPLOYEE_HAS_ASSIGNED_ASSETS",
                            "message", "Nhân viên này đang giữ " + assetCount
                                    + " thiết bị, vui lòng thu hồi qua chức năng Force-return"));
            return ResponseEntity.ok(ApiResponse.success(data));
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of("id", id, "deletedAt", deletedAt)));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin reset mật khẩu — trả temporaryPassword")
    public ResponseEntity<ApiResponse<ResetPasswordResponse>> resetPassword(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.resetPassword(id)));
    }

    @PutMapping("/me/change-password")
    @Operation(summary = "Đổi mật khẩu cá nhân — bắt buộc khi mustChangePassword=true")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            // [C3] Đọc Refresh Token cookie để revoke session cũ TRỪ phiên hiện tại
            @CookieValue(name = "refreshToken", required = false) String refreshTokenCookie) {
        String currentTokenHash = refreshTokenCookie != null ? sha256(refreshTokenCookie) : null;
        employeeService.changePassword(currentUser.getEmployeeId(), request, currentTokenHash);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── Private ───────────────────────────────────────────────────────────────

    /** SHA-256 hex — dùng để hash refresh token cookie trước khi truyền xuống Service */
    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
