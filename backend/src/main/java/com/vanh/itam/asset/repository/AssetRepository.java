package com.vanh.itam.asset.repository;

import com.vanh.itam.asset.entity.Asset;
import com.vanh.itam.asset.entity.AssetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    /** Tìm asset theo ID, chưa bị soft-delete — JOIN FETCH để tránh LazyInitializationException khi mapper truy cập */
    @Query("SELECT a FROM Asset a " +
           "JOIN FETCH a.category " +
           "JOIN FETCH a.branch " +
           "LEFT JOIN FETCH a.assignedTo " +
           "WHERE a.id = :id AND a.deletedAt IS NULL")
    Optional<Asset> findActiveById(@Param("id") Long id);

    /** Tìm asset theo mã code */
    @Query("SELECT a FROM Asset a WHERE a.code = :code AND a.deletedAt IS NULL")
    Optional<Asset> findByCode(@Param("code") String code);

    /** Danh sách asset theo chi nhánh, chưa bị soft-delete, có phân trang + filter */
    @Query(value = "SELECT a FROM Asset a " +
           "JOIN FETCH a.category " +
           "JOIN FETCH a.branch " +
           "LEFT JOIN FETCH a.assignedTo " +
           "WHERE a.deletedAt IS NULL " +
           "AND (:branchId IS NULL OR a.branch.id = :branchId) " +
           "AND (:status IS NULL OR a.status = :status) " +
           "AND (:categoryId IS NULL OR a.category.id = :categoryId) " +
           "AND (:assignedToId IS NULL OR a.assignedTo.id = :assignedToId)",
           countQuery = "SELECT COUNT(a) FROM Asset a WHERE a.deletedAt IS NULL " +
           "AND (:branchId IS NULL OR a.branch.id = :branchId) " +
           "AND (:status IS NULL OR a.status = :status) " +
           "AND (:categoryId IS NULL OR a.category.id = :categoryId) " +
           "AND (:assignedToId IS NULL OR a.assignedTo.id = :assignedToId)")
    Page<Asset> findAllActive(
            @Param("branchId") Long branchId,
            @Param("status") AssetStatus status,
            @Param("categoryId") Long categoryId,
            @Param("assignedToId") Long assignedToId,
            Pageable pageable);

    /** Danh sách asset đang active của 1 chi nhánh (dùng cho audit complete) */
    @Query("SELECT a FROM Asset a WHERE a.branch.id = :branchId " +
           "AND a.deletedAt IS NULL AND a.status != 'DISPOSED'")
    List<Asset> findActiveAssetsByBranch(@Param("branchId") Long branchId);

    /** Lấy sequence tiếp theo cho asset code (theo branchId + categoryId) */
    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(code FROM '\\d+$') AS INTEGER)), 0) + 1 " +
                   "FROM assets WHERE branch_id = :branchId AND category_id = :categoryId",
           nativeQuery = true)
    int findNextSequence(@Param("branchId") Long branchId, @Param("categoryId") Long categoryId);

    /** Kiểm tra code đã tồn tại chưa */
    boolean existsByCode(String code);

    /** Asset được assigned cho employee */
    @Query("SELECT a FROM Asset a WHERE a.assignedTo.id = :employeeId AND a.deletedAt IS NULL")
    List<Asset> findByAssignedToId(@Param("employeeId") Long employeeId);

    /** Đếm asset đang giữ bởi employee */
    @Query("SELECT COUNT(a) FROM Asset a WHERE a.assignedTo.id = :employeeId AND a.deletedAt IS NULL")
    long countByAssignedToId(@Param("employeeId") Long employeeId);

    /** Đếm asset đang dùng category (để check trước khi soft-delete category) */
    @Query("SELECT COUNT(a) FROM Asset a WHERE a.category.id = :categoryId AND a.deletedAt IS NULL")
    long countActiveByCategoryId(@Param("categoryId") Long categoryId);

    // [H3] COUNT queries cho enrichSessionResponse — tránh load toàn bộ list chỉ để .size()
    /** Đếm asset active của 1 chi nhánh (không tính DISPOSED) */
    @Query("SELECT COUNT(a) FROM Asset a WHERE a.branch.id = :branchId " +
           "AND a.deletedAt IS NULL AND a.status != 'DISPOSED'")
    int countActiveAssetsByBranch(@Param("branchId") Long branchId);
}
