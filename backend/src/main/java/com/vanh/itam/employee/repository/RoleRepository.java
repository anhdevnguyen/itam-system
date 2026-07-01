package com.vanh.itam.employee.repository;

import com.vanh.itam.employee.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho bảng roles — seed data cố định (4 roles: ADMIN, IT_STAFF, MANAGER, EMPLOYEE).
 * Không có soft delete — roles không bị xoá.
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Tìm role theo mã code (ADMIN, IT_STAFF, MANAGER, EMPLOYEE).
     * Dùng khi cần validate/gán role theo code string thay vì ID.
     */
    Optional<Role> findByCode(String code);

    /**
     * Kiểm tra role với code cho trước có tồn tại không.
     * Dùng để validate input trước khi thực hiện các thao tác liên quan đến role.
     */
    boolean existsByCode(String code);

    /**
     * Lấy toàn bộ danh sách roles (dùng cho dropdown UI).
     * Roles không có soft delete nên không cần filter deletedAt.
     * Kết quả sắp xếp theo tên hiển thị để UI nhất quán.
     */
    List<Role> findAllByOrderByNameAsc();
}
