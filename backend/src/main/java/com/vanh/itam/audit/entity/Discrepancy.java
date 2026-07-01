package com.vanh.itam.audit.entity;

import com.vanh.itam.asset.entity.Asset;
import com.vanh.itam.common.entity.BaseEntity;
import com.vanh.itam.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "discrepancies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Discrepancy extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_session_id", nullable = false)
    private AuditSession auditSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscrepancyType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscrepancyStatus status = DiscrepancyStatus.OPEN;

    @Column(name = "expected_location", length = 255)
    private String expectedLocation;

    @Column(name = "actual_location", length = 255)
    private String actualLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_action", length = 20)
    private ResolutionAction resolutionAction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private Employee resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
