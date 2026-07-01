package com.vanh.itam.request.repository;

import com.vanh.itam.request.entity.Request;
import com.vanh.itam.request.entity.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequestRepository extends JpaRepository<Request, Long> {

    /** Tìm request theo ID chưa soft-delete */
    @Query("SELECT r FROM Request r WHERE r.id = :id AND r.deletedAt IS NULL")
    java.util.Optional<Request> findActiveById(@Param("id") Long id);

    /** Danh sách request có phân trang + filter */
    @Query("SELECT r FROM Request r WHERE r.deletedAt IS NULL " +
           "AND (:status IS NULL OR r.status = :status) " +
           "AND (:employeeId IS NULL OR r.employee.id = :employeeId) " +
           "AND (:branchId IS NULL OR r.asset.branch.id = :branchId)")
    Page<Request> findAllActive(
            @Param("status") RequestStatus status,
            @Param("employeeId") Long employeeId,
            @Param("branchId") Long branchId,
            Pageable pageable);

    /**
     * Kiểm tra asset có request PENDING hoặc APPROVED nào đang mở không.
     * Dùng để chặn tạo request mới cho asset đã "giữ chỗ".
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
           "FROM Request r WHERE r.asset.id = :assetId " +
           "AND r.status IN ('PENDING', 'APPROVED') AND r.deletedAt IS NULL")
    boolean existsActiveRequestForAsset(@Param("assetId") Long assetId);
}
