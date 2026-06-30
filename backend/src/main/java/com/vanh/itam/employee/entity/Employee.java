package com.vanh.itam.employee.entity;

import com.vanh.itam.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity bảng employees.
 * email dùng làm username đăng nhập.
 * password_hash lưu BCrypt hash — KHÔNG bao giờ trả về qua API.
 */
@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
public class Employee extends BaseEntity {

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /** BCrypt hash — KHÔNG serialize ra JSON */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    /** FK → roles */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    /** FK → branches */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    /**
     * FK → departments — nullable.
     * Admin/IT Staff trung tâm có thể không gắn department.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    /**
     * true = buộc đổi mật khẩu ở lần đăng nhập kế tiếp.
     * Mặc định true khi tạo tài khoản mới / reset password.
     */
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = true;
}
