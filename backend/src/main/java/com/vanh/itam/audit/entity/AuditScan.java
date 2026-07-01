package com.vanh.itam.audit.entity;

import com.vanh.itam.asset.entity.Asset;
import com.vanh.itam.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "audit_scans")
@Getter
@Setter
@NoArgsConstructor
public class AuditScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_session_id", nullable = false)
    private AuditSession auditSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scanned_by", nullable = false)
    private Employee scannedBy;

    @Column(name = "scanned_location", length = 255)
    private String scannedLocation;

    @CreationTimestamp
    @Column(name = "scanned_at", nullable = false, updatable = false)
    private Instant scannedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
