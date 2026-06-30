package com.vanh.itam.employee.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Entity bảng roles — seed data cố định, không có deletedAt.
 * 4 role: ADMIN, IT_STAFF, MANAGER, EMPLOYEE.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã role: ADMIN, IT_STAFF, MANAGER, EMPLOYEE */
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    /** Tên hiển thị Tiếng Việt */
    @Column(nullable = false, length = 100)
    private String name;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
