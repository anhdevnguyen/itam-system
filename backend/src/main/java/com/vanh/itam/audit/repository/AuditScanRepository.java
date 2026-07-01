package com.vanh.itam.audit.repository;

import com.vanh.itam.audit.entity.AuditScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface AuditScanRepository extends JpaRepository<AuditScan, Long> {

    /** Lấy tập hợp assetId đã quét trong 1 session */
    @Query("SELECT s.asset.id FROM AuditScan s WHERE s.auditSession.id = :sessionId AND s.deletedAt IS NULL")
    Set<Long> findAssetIdsBySession(@Param("sessionId") Long sessionId);

    /** Kiểm tra asset đã được quét trong session chưa */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM AuditScan s WHERE s.auditSession.id = :sessionId AND s.asset.id = :assetId AND s.deletedAt IS NULL")
    boolean existsBySessionAndAsset(@Param("sessionId") Long sessionId, @Param("assetId") Long assetId);

    // [H3] COUNT query — tránh load Set<Long> để gọi .size() khi enrichSessionResponse
    /** Đếm số asset đã quét trong 1 session */
    @Query("SELECT COUNT(DISTINCT s.asset.id) FROM AuditScan s WHERE s.auditSession.id = :sessionId AND s.deletedAt IS NULL")
    int countScannedAssetsBySession(@Param("sessionId") Long sessionId);
}
