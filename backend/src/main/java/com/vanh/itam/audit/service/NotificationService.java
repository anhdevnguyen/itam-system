package com.vanh.itam.audit.service;

import com.vanh.itam.audit.dto.response.NotificationResponse;
import com.vanh.itam.audit.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    /** Tạo in-app notification cho 1 employee */
    void notify(Long employeeId, NotificationType type, String message);

    /** Tạo notification có kèm relatedEntityId */
    void notify(Long employeeId, NotificationType type, String message, Long relatedEntityId);

    Page<NotificationResponse> getMyNotifications(Long employeeId, Pageable pageable);
    long countUnread(Long employeeId);
    void markAsRead(Long notificationId, Long employeeId);
    void markAllAsRead(Long employeeId);
}
