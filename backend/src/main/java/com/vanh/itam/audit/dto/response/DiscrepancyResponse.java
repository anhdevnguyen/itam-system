package com.vanh.itam.audit.dto.response;

import com.vanh.itam.audit.entity.DiscrepancyStatus;
import com.vanh.itam.audit.entity.DiscrepancyType;
import com.vanh.itam.audit.entity.ResolutionAction;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class DiscrepancyResponse {
    private Long id;
    private Long auditSessionId;
    private Long assetId;
    private String assetCode;
    private String assetName;
    private DiscrepancyType type;
    private DiscrepancyStatus status;
    private String expectedLocation;
    private String actualLocation;
    private ResolutionAction resolutionAction;
    private Long resolvedById;
    private String resolvedByName;
    private Instant resolvedAt;
    private Instant createdAt;
}
