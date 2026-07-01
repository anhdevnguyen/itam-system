package com.vanh.itam.audit.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AuditScanResponse {
    private Long id;
    private Long auditSessionId;
    private Long assetId;
    private String assetCode;
    private String assetName;
    private Long scannedById;
    private String scannedByName;
    private String scannedLocation;
    private Instant scannedAt;
}
