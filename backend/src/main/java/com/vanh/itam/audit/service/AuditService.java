package com.vanh.itam.audit.service;

import com.vanh.itam.audit.dto.request.CreateAuditSessionRequest;
import com.vanh.itam.audit.dto.request.ResolveDiscrepancyRequest;
import com.vanh.itam.audit.dto.request.ScanRequest;
import com.vanh.itam.audit.dto.response.AuditScanResponse;
import com.vanh.itam.audit.dto.response.AuditSessionResponse;
import com.vanh.itam.audit.dto.response.DiscrepancyResponse;
import com.vanh.itam.audit.entity.DiscrepancyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditService {
    Page<AuditSessionResponse> getAllSessions(Long branchId, Pageable pageable);
    AuditSessionResponse getSessionById(Long id);
    AuditSessionResponse createSession(CreateAuditSessionRequest request, Long createdByEmployeeId);
    AuditScanResponse scan(Long sessionId, ScanRequest request, Long scannedByEmployeeId);
    AuditSessionResponse complete(Long sessionId);
    Page<DiscrepancyResponse> getDiscrepancies(Long sessionId, DiscrepancyStatus status, Pageable pageable);
    DiscrepancyResponse resolveDiscrepancy(Long discrepancyId, ResolveDiscrepancyRequest request, Long resolverId);
}
