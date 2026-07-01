package com.vanh.itam.asset.entity;

import com.vanh.itam.common.entity.BaseEntity;
import com.vanh.itam.employee.entity.Employee;
import com.vanh.itam.request.entity.Request;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "asset_assignment_history")
@Getter
@Setter
@NoArgsConstructor
public class AssetAssignmentHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    private Request request;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt = Instant.now();

    @Column(name = "returned_at")
    private Instant returnedAt;
}
