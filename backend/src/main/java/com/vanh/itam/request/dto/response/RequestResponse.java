package com.vanh.itam.request.dto.response;

import com.vanh.itam.request.entity.RequestStatus;
import com.vanh.itam.request.entity.RequestType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class RequestResponse {
    private Long id;
    private RequestType type;
    private RequestStatus status;
    private Long assetId;
    private String assetCode;
    private String assetName;
    private Long employeeId;
    private String employeeName;
    private Long approvedById;
    private String approvedByName;
    private Long fulfilledById;
    private String fulfilledByName;
    private String note;
    private String rejectionReason;
    private Instant approvedAt;
    private Instant fulfilledAt;
    private Instant createdAt;
    private Instant updatedAt;
}
