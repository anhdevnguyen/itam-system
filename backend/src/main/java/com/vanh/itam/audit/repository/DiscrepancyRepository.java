package com.vanh.itam.audit.repository;

import com.vanh.itam.audit.entity.Discrepancy;
import com.vanh.itam.audit.entity.DiscrepancyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DiscrepancyRepository extends JpaRepository<Discrepancy, Long> {

    @Query("SELECT d FROM Discrepancy d WHERE d.id = :id AND d.deletedAt IS NULL")
    Optional<Discrepancy> findActiveById(@Param("id") Long id);

    @Query("SELECT d FROM Discrepancy d WHERE d.auditSession.id = :sessionId AND d.deletedAt IS NULL " +
           "AND (:status IS NULL OR d.status = :status)")
    Page<Discrepancy> findBySession(
            @Param("sessionId") Long sessionId,
            @Param("status") DiscrepancyStatus status,
            Pageable pageable);
}
