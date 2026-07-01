package com.vanh.itam.request.entity;

import com.vanh.itam.asset.entity.Asset;
import com.vanh.itam.common.entity.BaseEntity;
import com.vanh.itam.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "requests")
@Getter
@Setter
@NoArgsConstructor
public class Request extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RequestType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status = RequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private Employee approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fulfilled_by")
    private Employee fulfilledBy;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "fulfilled_at")
    private Instant fulfilledAt;
}
