package com.vanh.itam.employee.repository;

import com.vanh.itam.employee.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    @Query("SELECT d FROM Department d WHERE d.deletedAt IS NULL " +
           "AND (:branchId IS NULL OR d.branch.id = :branchId) ORDER BY d.name")
    List<Department> findAllActive(@Param("branchId") Long branchId);

    @Query("SELECT d FROM Department d WHERE d.id = :id AND d.deletedAt IS NULL")
    Optional<Department> findActiveById(@Param("id") Long id);

    @Query("SELECT d FROM Department d WHERE d.deletedAt IS NULL " +
           "AND (:branchId IS NULL OR d.branch.id = :branchId)")
    Page<Department> findAllActivePaged(@Param("branchId") Long branchId, Pageable pageable);

    /** Đếm employee thuộc department (để check trước khi xóa) */
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.department.id = :departmentId AND e.deletedAt IS NULL")
    long countActiveEmployeesByDepartmentId(@Param("departmentId") Long departmentId);
}
