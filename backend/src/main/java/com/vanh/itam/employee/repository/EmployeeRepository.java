package com.vanh.itam.employee.repository;

import com.vanh.itam.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /**
     * Tìm employee theo email (dùng cho đăng nhập).
     * Chỉ lấy employee chưa bị soft-delete.
     */
    @Query("SELECT e FROM Employee e WHERE e.email = :email AND e.deletedAt IS NULL")
    Optional<Employee> findByEmailAndNotDeleted(String email);

    /** Kiểm tra email đã tồn tại chưa (kể cả đã soft-delete — tránh tái dùng email). */
    boolean existsByEmail(String email);
}
