package com.vanh.itam.employee.repository;

import com.vanh.itam.employee.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /**
     * Tìm employee theo email, chưa soft-delete (dùng cho login).
     * JOIN FETCH role + branch + department để tránh LazyInitializationException
     * khi mapper/service truy cập các field này ngoài transaction.
     */
    @Query("SELECT e FROM Employee e " +
           "JOIN FETCH e.role " +
           "JOIN FETCH e.branch " +
           "LEFT JOIN FETCH e.department " +
           "WHERE e.email = :email AND e.deletedAt IS NULL")
    Optional<Employee> findByEmailAndNotDeleted(@Param("email") String email);

    /**
     * Tìm employee theo ID, chưa soft-delete.
     * JOIN FETCH role + branch + department — cần thiết vì EmployeeMapper truy cập
     * role.code, role.id, branch.id, branch.name, department.id, department.name.
     */
    @Query("SELECT e FROM Employee e " +
           "JOIN FETCH e.role " +
           "JOIN FETCH e.branch " +
           "LEFT JOIN FETCH e.department " +
           "WHERE e.id = :id AND e.deletedAt IS NULL")
    Optional<Employee> findActiveById(@Param("id") Long id);

    /**
     * Dùng riêng cho JwtAuthenticationFilter — load role + branch trong 1 query.
     * Không filter deletedAt — filter check thủ công sau để log đúng case bị xóa.
     * Không cần LEFT JOIN department vì CustomUserDetails không dùng department.
     */
    @Query("SELECT e FROM Employee e " +
           "JOIN FETCH e.role " +
           "JOIN FETCH e.branch " +
           "WHERE e.id = :id")
    Optional<Employee> findByIdWithRoleAndBranch(@Param("id") Long id);

    /** Kiểm tra email đã tồn tại (kể cả đã soft-delete — tránh tái dùng email) */
    boolean existsByEmail(String email);

    /** Danh sách employee có filter + JOIN FETCH để tránh LazyInitializationException khi mapper truy cập role/branch/department */
    @Query(value = "SELECT e FROM Employee e " +
           "JOIN FETCH e.role " +
           "JOIN FETCH e.branch " +
           "LEFT JOIN FETCH e.department " +
           "WHERE e.deletedAt IS NULL " +
           "AND (:branchId IS NULL OR e.branch.id = :branchId) " +
           "AND (:departmentId IS NULL OR e.department.id = :departmentId) " +
           "AND (:roleCode IS NULL OR e.role.code = :roleCode)",
           countQuery = "SELECT COUNT(e) FROM Employee e WHERE e.deletedAt IS NULL " +
           "AND (:branchId IS NULL OR e.branch.id = :branchId) " +
           "AND (:departmentId IS NULL OR e.department.id = :departmentId) " +
           "AND (:roleCode IS NULL OR e.role.code = :roleCode)")
    Page<Employee> findAllActive(
            @Param("branchId") Long branchId,
            @Param("departmentId") Long departmentId,
            @Param("roleCode") String roleCode,
            Pageable pageable);
}
