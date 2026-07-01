package com.vanh.itam.maintenance.repository;

import com.vanh.itam.maintenance.entity.MaintenanceRecord;
import com.vanh.itam.maintenance.entity.MaintenanceStatus;
import com.vanh.itam.maintenance.entity.MaintenanceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MaintenanceRepository extends JpaRepository<MaintenanceRecord, Long> {

    @Query("SELECT m FROM MaintenanceRecord m WHERE m.id = :id AND m.deletedAt IS NULL")
    Optional<MaintenanceRecord> findActiveById(@Param("id") Long id);

    @Query("SELECT m FROM MaintenanceRecord m WHERE m.deletedAt IS NULL " +
           "AND (:assetId IS NULL OR m.asset.id = :assetId) " +
           "AND (:status IS NULL OR m.status = :status) " +
           "AND (:type IS NULL OR m.type = :type)")
    Page<MaintenanceRecord> findAllActive(
            @Param("assetId") Long assetId,
            @Param("status") MaintenanceStatus status,
            @Param("type") MaintenanceType type,
            Pageable pageable);
}
