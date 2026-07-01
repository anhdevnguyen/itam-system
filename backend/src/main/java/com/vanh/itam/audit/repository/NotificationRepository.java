package com.vanh.itam.audit.repository;

import com.vanh.itam.audit.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.employee.id = :employeeId AND n.deletedAt IS NULL " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> findByEmployeeId(@Param("employeeId") Long employeeId, Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.employee.id = :employeeId " +
           "AND n.isRead = false AND n.deletedAt IS NULL")
    long countUnreadByEmployeeId(@Param("employeeId") Long employeeId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.employee.id = :employeeId")
    void markAsRead(@Param("id") Long id, @Param("employeeId") Long employeeId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.employee.id = :employeeId AND n.isRead = false AND n.deletedAt IS NULL")
    void markAllAsRead(@Param("employeeId") Long employeeId);
}
