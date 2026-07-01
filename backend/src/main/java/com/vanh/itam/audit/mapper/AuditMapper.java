package com.vanh.itam.audit.mapper;

import com.vanh.itam.audit.dto.response.AuditScanResponse;
import com.vanh.itam.audit.dto.response.AuditSessionResponse;
import com.vanh.itam.audit.dto.response.DiscrepancyResponse;
import com.vanh.itam.audit.dto.response.NotificationResponse;
import com.vanh.itam.audit.entity.AuditScan;
import com.vanh.itam.audit.entity.AuditSession;
import com.vanh.itam.audit.entity.Discrepancy;
import com.vanh.itam.audit.entity.Notification;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AuditMapper {

    @Mapping(target = "branchId", source = "branch.id")
    @Mapping(target = "branchName", source = "branch.name")
    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "createdByName", source = "createdBy.fullName")
    @Mapping(target = "totalScanned", ignore = true)
    @Mapping(target = "totalAssets", ignore = true)
    AuditSessionResponse toResponse(AuditSession session);

    @Mapping(target = "auditSessionId", source = "auditSession.id")
    @Mapping(target = "assetId", source = "asset.id")
    @Mapping(target = "assetCode", source = "asset.code")
    @Mapping(target = "assetName", source = "asset.name")
    @Mapping(target = "scannedById", source = "scannedBy.id")
    @Mapping(target = "scannedByName", source = "scannedBy.fullName")
    AuditScanResponse toScanResponse(AuditScan scan);

    @Mapping(target = "auditSessionId", source = "auditSession.id")
    @Mapping(target = "assetId", source = "asset.id")
    @Mapping(target = "assetCode", source = "asset.code")
    @Mapping(target = "assetName", source = "asset.name")
    @Mapping(target = "resolvedById", source = "resolvedBy.id")
    @Mapping(target = "resolvedByName", source = "resolvedBy.fullName")
    DiscrepancyResponse toDiscrepancyResponse(Discrepancy discrepancy);

    @Mapping(target = "read", source = "read")
    NotificationResponse toNotificationResponse(Notification notification);
}
