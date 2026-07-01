package com.vanh.itam.audit.service;

import com.vanh.itam.asset.entity.Asset;
import com.vanh.itam.asset.entity.AssetStatus;
import com.vanh.itam.asset.exception.AssetNotFoundException;
import com.vanh.itam.asset.repository.AssetRepository;
import com.vanh.itam.audit.dto.request.CreateAuditSessionRequest;
import com.vanh.itam.audit.dto.request.ResolveDiscrepancyRequest;
import com.vanh.itam.audit.dto.request.ScanRequest;
import com.vanh.itam.audit.dto.response.AuditScanResponse;
import com.vanh.itam.audit.dto.response.AuditSessionResponse;
import com.vanh.itam.audit.dto.response.DiscrepancyResponse;
import com.vanh.itam.audit.entity.*;
import com.vanh.itam.audit.exception.AuditSessionAlreadyCompletedException;
import com.vanh.itam.audit.exception.AuditSessionNotFoundException;
import com.vanh.itam.audit.exception.DiscrepancyNotFoundException;
import com.vanh.itam.audit.mapper.AuditMapper;
import com.vanh.itam.audit.repository.*;
import com.vanh.itam.common.exception.BusinessException;
import com.vanh.itam.employee.entity.Branch;
import com.vanh.itam.employee.entity.Employee;
import com.vanh.itam.employee.exception.BranchNotFoundException;
import com.vanh.itam.employee.exception.EmployeeNotFoundException;
import com.vanh.itam.employee.repository.BranchRepository;
import com.vanh.itam.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final AuditSessionRepository sessionRepository;
    private final AuditScanRepository scanRepository;
    private final DiscrepancyRepository discrepancyRepository;
    private final AssetRepository assetRepository;
    private final BranchRepository branchRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditMapper auditMapper;
    private final NotificationService notificationService;

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    public Page<AuditSessionResponse> getAllSessions(Long branchId, Pageable pageable) {
        return sessionRepository.findAllActive(branchId, pageable)
                .map(session -> enrichSessionResponse(session));
    }

    @Override
    public AuditSessionResponse getSessionById(Long id) {
        AuditSession session = findSessionOrThrow(id);
        return enrichSessionResponse(session);
    }

    // ── Create Session ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuditSessionResponse createSession(CreateAuditSessionRequest request, Long createdByEmployeeId) {
        Branch branch = branchRepository.findActiveById(request.getBranchId())
                .orElseThrow(() -> new BranchNotFoundException(request.getBranchId()));
        Employee createdBy = employeeRepository.findActiveById(createdByEmployeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(createdByEmployeeId));

        AuditSession session = new AuditSession();
        session.setBranch(branch);
        session.setCreatedBy(createdBy);
        session.setNote(request.getNote());
        session.setStatus(AuditSessionStatus.IN_PROGRESS);
        session.setStartedAt(Instant.now());
        // expires_at = started_at + 3 ngày (docs 07 mục 4.5)
        session.setExpiresAt(Instant.now().plus(3, ChronoUnit.DAYS));

        AuditSession saved = sessionRepository.save(session);
        log.info("AuditSession created: id={}, branchId={}, createdBy={}",
                saved.getId(), branch.getId(), createdByEmployeeId);
        return enrichSessionResponse(saved);
    }

    // ── Scan ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuditScanResponse scan(Long sessionId, ScanRequest request, Long scannedByEmployeeId) {
        AuditSession session = findSessionOrThrow(sessionId);

        if (session.getStatus() == AuditSessionStatus.COMPLETED) {
            throw new AuditSessionAlreadyCompletedException();
        }

        // Tìm asset theo code
        Asset asset = assetRepository.findByCode(request.getAssetCode())
                .orElseThrow(() -> new BusinessException("AUDIT_SCAN_ASSET_CODE_NOT_FOUND",
                        "Không tìm thấy thiết bị với mã đã quét: " + request.getAssetCode()));

        // Validate asset thuộc đúng chi nhánh đang kiểm kê
        if (!asset.getBranch().getId().equals(session.getBranch().getId())) {
            throw new BusinessException("AUDIT_SCAN_BRANCH_MISMATCH",
                    "Thiết bị này không thuộc chi nhánh đang kiểm kê");
        }

        Employee scannedBy = employeeRepository.findActiveById(scannedByEmployeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(scannedByEmployeeId));

        AuditScan scan = new AuditScan();
        scan.setAuditSession(session);
        scan.setAsset(asset);
        scan.setScannedBy(scannedBy);
        scan.setScannedLocation(request.getScannedLocation());
        AuditScan saved = scanRepository.save(scan);

        log.info("AuditScan recorded: sessionId={}, assetCode={}, scannedBy={}",
                sessionId, request.getAssetCode(), scannedByEmployeeId);
        return auditMapper.toScanResponse(saved);
    }

    // ── Complete ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuditSessionResponse complete(Long sessionId) {
        AuditSession session = findSessionOrThrow(sessionId);

        if (session.getStatus() == AuditSessionStatus.COMPLETED) {
            throw new AuditSessionAlreadyCompletedException();
        }

        // Toàn bộ asset thuộc chi nhánh (không DISPOSED, chưa soft-delete)
        List<Asset> branchAssets = assetRepository.findActiveAssetsByBranch(session.getBranch().getId());

        // Set assetId đã quét
        Set<Long> scannedAssetIds = scanRepository.findAssetIdsBySession(sessionId);

        int missingCount = 0;
        for (Asset asset : branchAssets) {
            if (!scannedAssetIds.contains(asset.getId())) {
                // Tự động tạo discrepancy MISSING
                Discrepancy discrepancy = Discrepancy.builder()
                        .auditSession(session)
                        .asset(asset)
                        .type(DiscrepancyType.MISSING)
                        .status(DiscrepancyStatus.OPEN)
                        .build();
                discrepancyRepository.save(discrepancy);
                missingCount++;

                log.warn("Discrepancy MISSING created: sessionId={}, assetId={}, assetCode={}",
                        sessionId, asset.getId(), asset.getCode());

                // Notify IT Staff (người tạo session)
                notificationService.notify(
                        session.getCreatedBy().getId(),
                        NotificationType.DISCREPANCY_FOUND,
                        "Phát hiện thiết bị " + asset.getCode() + " không quét được trong kiểm kê",
                        discrepancy.getId());
            }
        }

        session.setStatus(AuditSessionStatus.COMPLETED);
        session.setCompletedAt(Instant.now());
        sessionRepository.save(session);

        log.info("AuditSession completed: id={}, branchId={}, totalAssets={}, missingCount={}",
                sessionId, session.getBranch().getId(), branchAssets.size(), missingCount);

        return enrichSessionResponse(session);
    }

    // ── Discrepancies ─────────────────────────────────────────────────────────

    @Override
    public Page<DiscrepancyResponse> getDiscrepancies(Long sessionId, DiscrepancyStatus status, Pageable pageable) {
        findSessionOrThrow(sessionId); // validate session tồn tại
        return discrepancyRepository.findBySession(sessionId, status, pageable)
                .map(auditMapper::toDiscrepancyResponse);
    }

    @Override
    @Transactional
    public DiscrepancyResponse resolveDiscrepancy(Long discrepancyId,
                                                   ResolveDiscrepancyRequest request,
                                                   Long resolverId) {
        Discrepancy discrepancy = discrepancyRepository.findActiveById(discrepancyId)
                .orElseThrow(() -> new DiscrepancyNotFoundException(discrepancyId));

        if (discrepancy.getStatus() == DiscrepancyStatus.RESOLVED) {
            throw new BusinessException("DISCREPANCY_ALREADY_RESOLVED",
                    "Sai lệch này đã được xử lý trước đó");
        }

        // action chỉ hợp lệ cho type MISSING
        if (discrepancy.getType() == DiscrepancyType.MISSING && request.getAction() != null) {
            if (request.getAction() == ResolutionAction.CONFIRM_LOST) {
                Asset asset = assetRepository.findActiveById(discrepancy.getAsset().getId())
                        .orElseThrow(() -> new AssetNotFoundException(discrepancy.getAsset().getId()));
                asset.setStatus(AssetStatus.LOST);
                assetRepository.save(asset);
            }
            discrepancy.setResolutionAction(request.getAction());
        } else if (discrepancy.getType() != DiscrepancyType.MISSING && request.getAction() != null) {
            throw new BusinessException("DISCREPANCY_INVALID_ACTION_FOR_TYPE",
                    "Hành động xử lý không hợp lệ cho loại sai lệch này");
        }

        Employee resolver = employeeRepository.findActiveById(resolverId)
                .orElseThrow(() -> new EmployeeNotFoundException(resolverId));

        discrepancy.setStatus(DiscrepancyStatus.RESOLVED);
        discrepancy.setResolvedBy(resolver);
        discrepancy.setResolvedAt(Instant.now());
        discrepancyRepository.save(discrepancy);

        log.info("Discrepancy resolved: id={}, type={}, action={}, resolverId={}",
                discrepancyId, discrepancy.getType(), request.getAction(), resolverId);

        return auditMapper.toDiscrepancyResponse(discrepancy);
    }

    // ── Auto-expire Scheduler ─────────────────────────────────────────────────

    /**
     * Chạy mỗi giờ — tự động complete các audit session quá hạn 3 ngày.
     * Tái sử dụng complete() để đảm bảo logic nhất quán (docs 07 mục 4.5).
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void autoExpireAuditSessions() {
        List<AuditSession> expired = sessionRepository.findByStatusAndExpiresAtBefore(
                AuditSessionStatus.IN_PROGRESS, Instant.now());

        for (AuditSession session : expired) {
            log.info("Auto-expiring audit session: id={}, branchId={}",
                    session.getId(), session.getBranch().getId());
            try {
                complete(session.getId());
            } catch (Exception e) {
                log.error("Failed to auto-expire audit session: id={}", session.getId(), e);
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private AuditSession findSessionOrThrow(Long id) {
        return sessionRepository.findActiveById(id)
                .orElseThrow(() -> new AuditSessionNotFoundException(id));
    }

    /**
     * Enrich AuditSessionResponse với totalScanned và totalAssets.
     * MapStruct không tự tính các field này nên cần set thủ công sau mapping.
     */
    private AuditSessionResponse enrichSessionResponse(AuditSession session) {
        AuditSessionResponse base = auditMapper.toResponse(session);
        int totalAssets = assetRepository.findActiveAssetsByBranch(session.getBranch().getId()).size();
        int totalScanned = scanRepository.findAssetIdsBySession(session.getId()).size();
        return AuditSessionResponse.builder()
                .id(base.getId())
                .branchId(base.getBranchId())
                .branchName(base.getBranchName())
                .status(base.getStatus())
                .createdById(base.getCreatedById())
                .createdByName(base.getCreatedByName())
                .note(base.getNote())
                .startedAt(base.getStartedAt())
                .expiresAt(base.getExpiresAt())
                .completedAt(base.getCompletedAt())
                .createdAt(base.getCreatedAt())
                .totalAssets(totalAssets)
                .totalScanned(totalScanned)
                .build();
    }
}
