package com.vanh.itam.asset.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AssetAssignmentHistoryResponse {
    private Long id;
    private Long assetId;
    private Long employeeId;
    private String employeeName;
    private Long requestId;
    private Instant assignedAt;
    private Instant returnedAt;
}
