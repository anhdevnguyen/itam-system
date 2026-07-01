package com.vanh.itam.audit.service;

import com.vanh.itam.audit.dto.response.NotificationResponse;
import com.vanh.itam.audit.entity.Notification;
import com.vanh.itam.audit.entity.NotificationType;
import com.vanh.itam.audit.mapper.AuditMapper;
import com.vanh.itam.audit.repository.NotificationRepository;
import com.vanh.itam.employee.entity.Employee;
import com.vanh.itam.employee.exception.EmployeeNotFoundException;
import com.vanh.itam.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditMapper auditMapper;

    @Override
    @Transactional
    public void notify(Long employeeId, NotificationType type, String message) {
        notify(employeeId, type, message, null);
    }

    @Override
    @Transactional
    public void notify(Long employeeId, NotificationType type, String message, Long relatedEntityId) {
        Employee employee = employeeRepository.findActiveById(employeeId).orElse(null);
        if (employee == null) {
            log.warn("Cannot send notification — employee not found: employeeId={}", employeeId);
            return;
        }
        Notification notification = Notification.builder()
                .employee(employee)
                .type(type.name())
                .message(message)
                .isRead(false)
                .relatedEntityId(relatedEntityId)
                .build();
        notificationRepository.save(notification);
        log.debug("In-app notification created: employeeId={}, type={}", employeeId, type);
    }

    @Override
    public Page<NotificationResponse> getMyNotifications(Long employeeId, Pageable pageable) {
        return notificationRepository.findByEmployeeId(employeeId, pageable)
                .map(auditMapper::toNotificationResponse);
    }

    @Override
    public long countUnread(Long employeeId) {
        return notificationRepository.countUnreadByEmployeeId(employeeId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long employeeId) {
        notificationRepository.markAsRead(notificationId, employeeId);
    }
}
