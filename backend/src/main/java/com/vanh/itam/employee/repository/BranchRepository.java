package com.vanh.itam.employee.repository;

import com.vanh.itam.employee.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    @Query("SELECT b FROM Branch b WHERE b.deletedAt IS NULL ORDER BY b.name")
    List<Branch> findAllActive();

    @Query("SELECT b FROM Branch b WHERE b.id = :id AND b.deletedAt IS NULL")
    Optional<Branch> findActiveById(@Param("id") Long id);

    boolean existsByCode(String code);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Branch b WHERE b.code = :code AND b.id != :excludeId")
    boolean existsByCodeAndIdNot(@Param("code") String code, @Param("excludeId") Long excludeId);

    /** Đếm employee active thuộc branch (để check trước khi xóa branch) */
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.branch.id = :branchId AND e.deletedAt IS NULL")
    long countActiveEmployeesByBranchId(@Param("branchId") Long branchId);
}
