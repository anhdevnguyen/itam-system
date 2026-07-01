package com.vanh.itam.audit.controller;

import com.vanh.itam.audit.dto.response.NotificationResponse;
import com.vanh.itam.audit.service.NotificationService;
import com.vanh.itam.common.config.CustomUserDetails;
import com.vanh.itam.common.response.ApiResponse;
import com.vanh.itam.common.response.Pagination;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Thông báo in-app (polling)")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Danh sách thông báo của user đang đăng nhập")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<NotificationResponse> page = notificationService.getMyNotifications(
                currentUser.getEmployeeId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(page.getContent(),
                Pagination.of(page.getNumber(), page.getSize(),
                        page.getTotalElements(), page.getTotalPages())));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Số thông báo chưa đọc (FE polling 30-60s)")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countUnread(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        long count = notificationService.countUnread(currentUser.getEmployeeId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", count)));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Đánh dấu đã đọc 1 thông báo")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        notificationService.markAsRead(id, currentUser.getEmployeeId());
        return ResponseEntity.ok(ApiResponse.success());
    }
}
