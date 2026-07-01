package com.vanh.itam.asset.repository;

import com.vanh.itam.asset.entity.AssetAssignmentHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AssetAssignmentHistoryRepository extends JpaRepository<AssetAssignmentHistory, Long> {

    /** Tìm dòng lịch sử đang mở (returnedAt IS NULL) của asset */
    @Query("SELECT h FROM AssetAssignmentHistory h " +
           "JOIN FETCH h.asset " +
           "JOIN FETCH h.employee " +
           "LEFT JOIN FETCH h.request " +
           "WHERE h.asset.id = :assetId " +
           "AND h.returnedAt IS NULL AND h.deletedAt IS NULL")
    Optional<AssetAssignmentHistory> findOpenAssignment(@Param("assetId") Long assetId);

    /** Lịch sử cấp phát của 1 asset — JOIN FETCH để tránh LazyInitializationException khi mapper truy cập employee/request */
    @Query(value = "SELECT h FROM AssetAssignmentHistory h " +
           "JOIN FETCH h.asset " +
           "JOIN FETCH h.employee " +
           "LEFT JOIN FETCH h.request " +
           "WHERE h.asset.id = :assetId " +
           "AND h.deletedAt IS NULL ORDER BY h.assignedAt DESC",
           countQuery = "SELECT COUNT(h) FROM AssetAssignmentHistory h " +
           "WHERE h.asset.id = :assetId AND h.deletedAt IS NULL")
    Page<AssetAssignmentHistory> findByAssetId(@Param("assetId") Long assetId, Pageable pageable);
}
