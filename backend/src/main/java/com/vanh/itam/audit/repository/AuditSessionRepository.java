package com.vanh.itam.audit.repository;

import com.vanh.itam.audit.entity.AuditSession;
import com.vanh.itam.audit.entity.AuditSessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AuditSessionRepository extends JpaRepository<AuditSession, Long> {

    @Query("SELECT s FROM AuditSession s WHERE s.id = :id AND s.deletedAt IS NULL")
    Optional<AuditSession> findActiveById(@Param("id") Long id);

    @Query("SELECT s FROM AuditSession s WHERE s.deletedAt IS NULL " +
           "AND (:branchId IS NULL OR s.branch.id = :branchId)")
    Page<AuditSession> findAllActive(@Param("branchId") Long branchId, Pageable pageable);

    /** Tìm session quá hạn để auto-expire */
    @Query("SELECT s FROM AuditSession s WHERE s.status = :status AND s.expiresAt < :now AND s.deletedAt IS NULL")
    List<AuditSession> findByStatusAndExpiresAtBefore(
            @Param("status") AuditSessionStatus status,
            @Param("now") Instant now);
}
