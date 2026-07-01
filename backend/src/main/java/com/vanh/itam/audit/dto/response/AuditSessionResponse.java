package com.vanh.itam.audit.dto.response;

import com.vanh.itam.audit.entity.AuditSessionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AuditSessionResponse {
    private Long id;
    private Long branchId;
    private String branchName;
    private AuditSessionStatus status;
    private Long createdById;
    private String createdByName;
    private String note;
    private Instant startedAt;
    private Instant expiresAt;
    private Instant completedAt;
    private Instant createdAt;
    private int totalScanned;
    private int totalAssets;
}
