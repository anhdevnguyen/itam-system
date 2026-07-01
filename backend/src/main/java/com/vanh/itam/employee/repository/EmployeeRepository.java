package com.vanh.itam.employee.repository;

import com.vanh.itam.employee.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /** Tìm employee theo email, chưa soft-delete (dùng cho đăng nhập) */
    @Query("SELECT e FROM Employee e WHERE e.email = :email AND e.deletedAt IS NULL")
    Optional<Employee> findByEmailAndNotDeleted(@Param("email") String email);

    /** Tìm employee theo ID, chưa soft-delete */
    @Query("SELECT e FROM Employee e WHERE e.id = :id AND e.deletedAt IS NULL")
    Optional<Employee> findActiveById(@Param("id") Long id);

    /** Kiểm tra email đã tồn tại (kể cả đã soft-delete — tránh tái dùng email) */
    boolean existsByEmail(String email);

    /** Danh sách employee có filter */
    @Query("SELECT e FROM Employee e WHERE e.deletedAt IS NULL " +
           "AND (:branchId IS NULL OR e.branch.id = :branchId) " +
           "AND (:departmentId IS NULL OR e.department.id = :departmentId) " +
           "AND (:roleCode IS NULL OR e.role.code = :roleCode)")
    Page<Employee> findAllActive(
            @Param("branchId") Long branchId,
            @Param("departmentId") Long departmentId,
            @Param("roleCode") String roleCode,
            Pageable pageable);
}
