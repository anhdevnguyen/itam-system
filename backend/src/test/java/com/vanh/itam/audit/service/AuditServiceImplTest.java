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
import com.vanh.itam.audit.repository.AuditScanRepository;
import com.vanh.itam.audit.repository.AuditSessionRepository;
import com.vanh.itam.audit.repository.DiscrepancyRepository;
import com.vanh.itam.common.exception.BusinessException;
import com.vanh.itam.employee.entity.Branch;
import com.vanh.itam.employee.entity.Employee;
import com.vanh.itam.employee.exception.BranchNotFoundException;
import com.vanh.itam.employee.exception.EmployeeNotFoundException;
import com.vanh.itam.employee.repository.BranchRepository;
import com.vanh.itam.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho AuditServiceImpl.
 * Mock toàn bộ Repository và external service — không chạm DB thật.
 * Kiểm tra logic nghiệp vụ theo docs/07-BUSINESS-RULES.md mục 4.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditServiceImpl — Unit Tests")
class AuditServiceImplTest {

    @Mock private AuditSessionRepository sessionRepository;
    @Mock private AuditScanRepository scanRepository;
    @Mock private DiscrepancyRepository discrepancyRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private AuditMapper auditMapper;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private AuditServiceImpl auditService;

    // ── Helpers ────────────────────────────────────────────────────────────

    private Branch buildBranch(Long id) {
        Branch b = new Branch();
        b.setId(id);
        b.setCode("HN");
        b.setName("Hà Nội");
        return b;
    }

    private Employee buildEmployee(Long id) {
        Employee e = new Employee();
        e.setId(id);
        e.setFullName("Test Employee " + id);
        return e;
    }

    private Asset buildAsset(Long id, Long branchId) {
        Asset a = new Asset();
        a.setId(id);
        a.setCode("HN-LAP-000" + id);
        a.setName("Asset " + id);
        a.setStatus(AssetStatus.AVAILABLE);
        Branch branch = buildBranch(branchId);
        a.setBranch(branch);
        return a;
    }

    private AuditSession buildSession(Long id, Long branchId, AuditSessionStatus status) {
        AuditSession s = new AuditSession();
        s.setId(id);
        s.setBranch(buildBranch(branchId));
        s.setCreatedBy(buildEmployee(1L));
        s.setStatus(status);
        s.setStartedAt(Instant.now());
        s.setExpiresAt(Instant.now().plusSeconds(86400 * 3));
        return s;
    }

    private Discrepancy buildDiscrepancy(Long id, DiscrepancyType type, DiscrepancyStatus status, Long assetId, Long sessionId) {
        Discrepancy d = new Discrepancy();
        d.setId(id);
        d.setType(type);
        d.setStatus(status);
        d.setAsset(buildAsset(assetId, 1L));
        AuditSession session = buildSession(sessionId, 1L, AuditSessionStatus.IN_PROGRESS);
        d.setAuditSession(session);
        return d;
    }

    // ── createSession ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("createSession()")
    class CreateSessionTests {

        @Test
        @DisplayName("Nên tạo session thành công với status IN_PROGRESS và expires_at = +3 ngày")
        void createSession_shouldSucceed_withCorrectStatusAndExpiry() {
            Long branchId = 1L;
            Long creatorId = 10L;

            Branch branch = buildBranch(branchId);
            Employee creator = buildEmployee(creatorId);
            AuditSession savedSession = buildSession(100L, branchId, AuditSessionStatus.IN_PROGRESS);
            AuditSessionResponse mockResponse = mock(AuditSessionResponse.class);

            when(branchRepository.findActiveById(branchId)).thenReturn(Optional.of(branch));
            when(employeeRepository.findActiveById(creatorId)).thenReturn(Optional.of(creator));
            when(sessionRepository.save(any(AuditSession.class))).thenReturn(savedSession);
            when(auditMapper.toResponse(any(AuditSession.class))).thenReturn(mockResponse);
            when(assetRepository.findActiveAssetsByBranch(branchId)).thenReturn(List.of());
            when(scanRepository.findAssetIdsBySession(anyLong())).thenReturn(Set.of());

            CreateAuditSessionRequest request = new CreateAuditSessionRequest();
            request.setBranchId(branchId);
            request.setNote("Kiểm kê tháng 7");

            AuditSessionResponse result = auditService.createSession(request, creatorId);

            assertThat(result).isNotNull();
            verify(sessionRepository).save(argThat(session ->
                    session.getStatus() == AuditSessionStatus.IN_PROGRESS
                    && session.getExpiresAt() != null
                    && session.getExpiresAt().isAfter(Instant.now())
            ));
        }

        @Test
        @DisplayName("Nên ném BranchNotFoundException khi branchId không tồn tại")
        void createSession_shouldThrow_whenBranchNotFound() {
            when(branchRepository.findActiveById(999L)).thenReturn(Optional.empty());

            CreateAuditSessionRequest request = new CreateAuditSessionRequest();
            request.setBranchId(999L);

            assertThatThrownBy(() -> auditService.createSession(request, 1L))
                    .isInstanceOf(BranchNotFoundException.class);
        }

        @Test
        @DisplayName("Nên ném EmployeeNotFoundException khi createdBy không tồn tại")
        void createSession_shouldThrow_whenCreatorNotFound() {
            Branch branch = buildBranch(1L);
            when(branchRepository.findActiveById(1L)).thenReturn(Optional.of(branch));
            when(employeeRepository.findActiveById(999L)).thenReturn(Optional.empty());

            CreateAuditSessionRequest request = new CreateAuditSessionRequest();
            request.setBranchId(1L);

            assertThatThrownBy(() -> auditService.createSession(request, 999L))
                    .isInstanceOf(EmployeeNotFoundException.class);
        }
    }

    // ── scan ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("scan()")
    class ScanTests {

        @Test
        @DisplayName("Nên ghi nhận scan thành công khi asset đúng chi nhánh")
        void scan_shouldSucceed_whenAssetBelongsToSessionBranch() {
            Long sessionId = 1L;
            Long scannedById = 5L;

            AuditSession session = buildSession(sessionId, 1L, AuditSessionStatus.IN_PROGRESS);
            Asset asset = buildAsset(10L, 1L); // cùng branch_id = 1
            Employee scanner = buildEmployee(scannedById);
            AuditScan savedScan = new AuditScan();
            AuditScanResponse mockResponse = mock(AuditScanResponse.class);

            when(sessionRepository.findActiveById(sessionId)).thenReturn(Optional.of(session));
            when(assetRepository.findByCode("HN-LAP-0010")).thenReturn(Optional.of(asset));
            when(employeeRepository.findActiveById(scannedById)).thenReturn(Optional.of(scanner));
            when(scanRepository.save(any(AuditScan.class))).thenReturn(savedScan);
            when(auditMapper.toScanResponse(savedScan)).thenReturn(mockResponse);

            ScanRequest request = new ScanRequest();
            request.setAssetCode("HN-LAP-0010");
            request.setScannedLocation("Phòng A1");

            AuditScanResponse result = auditService.scan(sessionId, request, scannedById);

            assertThat(result).isEqualTo(mockResponse);
            verify(scanRepository).save(any(AuditScan.class));
        }

        @Test
        @DisplayName("Nên ném BusinessException khi asset thuộc chi nhánh khác với session")
        void scan_shouldThrow_whenAssetBranchMismatch() {
            Long sessionId = 1L;

            AuditSession session = buildSession(sessionId, 1L, AuditSessionStatus.IN_PROGRESS); // branch 1
            Asset assetFromOtherBranch = buildAsset(20L, 2L); // branch 2 — khác!

            when(sessionRepository.findActiveById(sessionId)).thenReturn(Optional.of(session));
            when(assetRepository.findByCode("HCM-LAP-0001")).thenReturn(Optional.of(assetFromOtherBranch));

            ScanRequest request = new ScanRequest();
            request.setAssetCode("HCM-LAP-0001");

            assertThatThrownBy(() -> auditService.scan(sessionId, request, 5L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("không thuộc chi nhánh");
        }

        @Test
        @DisplayName("Nên ném AuditSessionAlreadyCompletedException khi session đã COMPLETED")
        void scan_shouldThrow_whenSessionAlreadyCompleted() {
            Long sessionId = 1L;
            AuditSession completedSession = buildSession(sessionId, 1L, AuditSessionStatus.COMPLETED);

            when(sessionRepository.findActiveById(sessionId)).thenReturn(Optional.of(completedSession));

            ScanRequest request = new ScanRequest();
            request.setAssetCode("HN-LAP-0001");

            assertThatThrownBy(() -> auditService.scan(sessionId, request, 5L))
                    .isInstanceOf(AuditSessionAlreadyCompletedException.class);
        }

        @Test
        @DisplayName("Nên ném BusinessException khi assetCode không tồn tại")
        void scan_shouldThrow_whenAssetCodeNotFound() {
            Long sessionId = 1L;
            AuditSession session = buildSession(sessionId, 1L, AuditSessionStatus.IN_PROGRESS);

            when(sessionRepository.findActiveById(sessionId)).thenReturn(Optional.of(session));
            when(assetRepository.findByCode("INVALID-CODE")).thenReturn(Optional.empty());

            ScanRequest request = new ScanRequest();
            request.setAssetCode("INVALID-CODE");

            assertThatThrownBy(() -> auditService.scan(sessionId, request, 5L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Nên ném AuditSessionNotFoundException khi sessionId không tồn tại")
        void scan_shouldThrow_whenSessionNotFound() {
            when(sessionRepository.findActiveById(999L)).thenReturn(Optional.empty());

            ScanRequest request = new ScanRequest();
            request.setAssetCode("HN-LAP-0001");

            assertThatThrownBy(() -> auditService.scan(999L, request, 5L))
                    .isInstanceOf(AuditSessionNotFoundException.class);
        }
    }

    // ── complete ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("complete()")
    class CompleteTests {

        @Test
        @DisplayName("Nên tạo discrepancy MISSING cho mỗi asset chi nhánh chưa được quét")
        void complete_shouldCreateMissingDiscrepancies_forUnscannedAssets() {
            Long sessionId = 1L;
            Long branchId = 1L;

            AuditSession session = buildSession(sessionId, branchId, AuditSessionStatus.IN_PROGRESS);

            // Chi nhánh có 3 assets
            Asset asset1 = buildAsset(1L, branchId);
            Asset asset2 = buildAsset(2L, branchId);
            Asset asset3 = buildAsset(3L, branchId);
            List<Asset> branchAssets = List.of(asset1, asset2, asset3);

            // Chỉ asset1 đã được quét
            Set<Long> scannedIds = Set.of(1L);

            AuditSessionResponse mockResponse = mock(AuditSessionResponse.class);

            when(sessionRepository.findActiveById(sessionId)).thenReturn(Optional.of(session));
            when(assetRepository.findActiveAssetsByBranch(branchId)).thenReturn(branchAssets);
            when(scanRepository.findAssetIdsBySession(sessionId)).thenReturn(scannedIds);
            when(sessionRepository.save(any())).thenReturn(session);
            when(auditMapper.toResponse(any())).thenReturn(mockResponse);
            when(discrepancyRepository.save(any())).thenAnswer(inv -> {
                Discrepancy d = inv.getArgument(0);
                d.setId(System.nanoTime()); // gán id giả để notify hoạt động
                return d;
            });

            AuditSessionResponse result = auditService.complete(sessionId);

            assertThat(result).isNotNull();
            // 2 asset chưa quét (asset2, asset3) → 2 discrepancy MISSING
            verify(discrepancyRepository, times(2)).save(argThat(d ->
                    d.getType() == DiscrepancyType.MISSING
                    && d.getStatus() == DiscrepancyStatus.OPEN
            ));
            // Notification cho creator (id=1) mỗi discrepancy
            verify(notificationService, times(2)).notify(
                    eq(session.getCreatedBy().getId()),
                    eq(NotificationType.DISCREPANCY_FOUND),
                    anyString(),
                    any()
            );
        }

        @Test
        @DisplayName("Nên không tạo discrepancy khi tất cả asset đều đã quét")
        void complete_shouldCreateNoDiscrepancies_whenAllAssetsScanned() {
            Long sessionId = 1L;
            Long branchId = 1L;

            AuditSession session = buildSession(sessionId, branchId, AuditSessionStatus.IN_PROGRESS);
            Asset asset1 = buildAsset(1L, branchId);
            Asset asset2 = buildAsset(2L, branchId);
            List<Asset> branchAssets = List.of(asset1, asset2);
            Set<Long> scannedIds = Set.of(1L, 2L); // tất cả đã quét

            AuditSessionResponse mockResponse = mock(AuditSessionResponse.class);

            when(sessionRepository.findActiveById(sessionId)).thenReturn(Optional.of(session));
            when(assetRepository.findActiveAssetsByBranch(branchId)).thenReturn(branchAssets);
            when(scanRepository.findAssetIdsBySession(sessionId)).thenReturn(scannedIds);
            when(sessionRepository.save(any())).thenReturn(session);
            when(auditMapper.toResponse(any())).thenReturn(mockResponse);

            auditService.complete(sessionId);

            verify(discrepancyRepository, never()).save(any());
            verify(notificationService, never()).notify(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Nên set status COMPLETED và completedAt sau khi complete")
        void complete_shouldSetSessionCompletedStatusAndTimestamp() {
            Long sessionId = 1L;
            Long branchId = 1L;

            AuditSession session = buildSession(sessionId, branchId, AuditSessionStatus.IN_PROGRESS);

            when(sessionRepository.findActiveById(sessionId)).thenReturn(Optional.of(session));
            when(assetRepository.findActiveAssetsByBranch(branchId)).thenReturn(List.of());
            when(scanRepository.findAssetIdsBySession(sessionId)).thenReturn(Set.of());
            when(sessionRepository.save(any())).thenReturn(session);
            when(auditMapper.toResponse(any())).thenReturn(mock(AuditSessionResponse.class));

            auditService.complete(sessionId);

            verify(sessionRepository).save(argThat(s ->
                    s.getStatus() == AuditSessionStatus.COMPLETED
                    && s.getCompletedAt() != null
            ));
        }

        @Test
        @DisplayName("Nên ném AuditSessionAlreadyCompletedException khi complete session đã COMPLETED")
        void complete_shouldThrow_whenSessionAlreadyCompleted() {
            AuditSession completedSession = buildSession(1L, 1L, AuditSessionStatus.COMPLETED);

            when(sessionRepository.findActiveById(1L)).thenReturn(Optional.of(completedSession));

            assertThatThrownBy(() -> auditService.complete(1L))
                    .isInstanceOf(AuditSessionAlreadyCompletedException.class);

            verify(discrepancyRepository, never()).save(any());
            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Nên không tạo discrepancy khi chi nhánh không có asset nào")
        void complete_shouldSucceed_whenBranchHasNoAssets() {
            Long sessionId = 1L;
            AuditSession session = buildSession(sessionId, 1L, AuditSessionStatus.IN_PROGRESS);

            when(sessionRepository.findActiveById(sessionId)).thenReturn(Optional.of(session));
            when(assetRepository.findActiveAssetsByBranch(1L)).thenReturn(List.of()); // không có asset
            when(scanRepository.findAssetIdsBySession(sessionId)).thenReturn(Set.of());
            when(sessionRepository.save(any())).thenReturn(session);
            when(auditMapper.toResponse(any())).thenReturn(mock(AuditSessionResponse.class));

            auditService.complete(sessionId);

            verify(discrepancyRepository, never()).save(any());
        }
    }

    // ── resolveDiscrepancy ────────────────────────────────────────────────

    @Nested
    @DisplayName("resolveDiscrepancy()")
    class ResolveDiscrepancyTests {

        @Test
        @DisplayName("CONFIRM_LOST: Nên set asset.status = LOST và mark discrepancy RESOLVED")
        void resolve_shouldSetAssetLost_whenActionIsConfirmLost() {
            Long discrepancyId = 1L;
            Long resolverId = 5L;

            Discrepancy discrepancy = buildDiscrepancy(discrepancyId, DiscrepancyType.MISSING,
                    DiscrepancyStatus.OPEN, 10L, 1L);
            Asset asset = buildAsset(10L, 1L);
            Employee resolver = buildEmployee(resolverId);
            DiscrepancyResponse mockResponse = mock(DiscrepancyResponse.class);

            when(discrepancyRepository.findActiveById(discrepancyId))
                    .thenReturn(Optional.of(discrepancy));
            when(assetRepository.findActiveById(10L)).thenReturn(Optional.of(asset));
            when(employeeRepository.findActiveById(resolverId)).thenReturn(Optional.of(resolver));
            when(discrepancyRepository.save(any())).thenReturn(discrepancy);
            when(assetRepository.save(any())).thenReturn(asset);
            when(auditMapper.toDiscrepancyResponse(any())).thenReturn(mockResponse);

            ResolveDiscrepancyRequest request = new ResolveDiscrepancyRequest();
            request.setAction(ResolutionAction.CONFIRM_LOST);

            DiscrepancyResponse result = auditService.resolveDiscrepancy(discrepancyId, request, resolverId);

            assertThat(result).isEqualTo(mockResponse);
            // Asset phải bị đổi status LOST
            assertThat(asset.getStatus()).isEqualTo(AssetStatus.LOST);
            // Discrepancy phải RESOLVED với action và resolver đúng
            assertThat(discrepancy.getStatus()).isEqualTo(DiscrepancyStatus.RESOLVED);
            assertThat(discrepancy.getResolutionAction()).isEqualTo(ResolutionAction.CONFIRM_LOST);
            assertThat(discrepancy.getResolvedBy()).isEqualTo(resolver);
            assertThat(discrepancy.getResolvedAt()).isNotNull();

            verify(assetRepository).save(asset);
        }

        @Test
        @DisplayName("FOUND: Nên KHÔNG đổi asset.status và chỉ mark discrepancy RESOLVED")
        void resolve_shouldNotChangeAsset_whenActionIsFound() {
            Long discrepancyId = 1L;
            Long resolverId = 5L;

            Discrepancy discrepancy = buildDiscrepancy(discrepancyId, DiscrepancyType.MISSING,
                    DiscrepancyStatus.OPEN, 10L, 1L);
            Employee resolver = buildEmployee(resolverId);
            DiscrepancyResponse mockResponse = mock(DiscrepancyResponse.class);

            when(discrepancyRepository.findActiveById(discrepancyId))
                    .thenReturn(Optional.of(discrepancy));
            when(employeeRepository.findActiveById(resolverId)).thenReturn(Optional.of(resolver));
            when(discrepancyRepository.save(any())).thenReturn(discrepancy);
            when(auditMapper.toDiscrepancyResponse(any())).thenReturn(mockResponse);

            ResolveDiscrepancyRequest request = new ResolveDiscrepancyRequest();
            request.setAction(ResolutionAction.FOUND);

            auditService.resolveDiscrepancy(discrepancyId, request, resolverId);

            // FOUND → không save asset, không đổi status
            verify(assetRepository, never()).save(any());
            assertThat(discrepancy.getStatus()).isEqualTo(DiscrepancyStatus.RESOLVED);
            assertThat(discrepancy.getResolutionAction()).isEqualTo(ResolutionAction.FOUND);
        }

        @Test
        @DisplayName("Nên ném BusinessException khi discrepancy đã RESOLVED")
        void resolve_shouldThrow_whenDiscrepancyAlreadyResolved() {
            Discrepancy alreadyResolved = buildDiscrepancy(1L, DiscrepancyType.MISSING,
                    DiscrepancyStatus.RESOLVED, 10L, 1L);

            when(discrepancyRepository.findActiveById(1L)).thenReturn(Optional.of(alreadyResolved));

            ResolveDiscrepancyRequest request = new ResolveDiscrepancyRequest();
            request.setAction(ResolutionAction.CONFIRM_LOST);

            assertThatThrownBy(() -> auditService.resolveDiscrepancy(1L, request, 5L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("đã được xử lý");

            verify(assetRepository, never()).save(any());
        }

        @Test
        @DisplayName("Nên ném DiscrepancyNotFoundException khi discrepancyId không tồn tại")
        void resolve_shouldThrow_whenDiscrepancyNotFound() {
            when(discrepancyRepository.findActiveById(999L)).thenReturn(Optional.empty());

            ResolveDiscrepancyRequest request = new ResolveDiscrepancyRequest();
            request.setAction(ResolutionAction.FOUND);

            assertThatThrownBy(() -> auditService.resolveDiscrepancy(999L, request, 5L))
                    .isInstanceOf(DiscrepancyNotFoundException.class);
        }

        @Test
        @DisplayName("Nên ném BusinessException khi gửi action cho discrepancy type LOCATION_MISMATCH")
        void resolve_shouldThrow_whenActionProvidedForNonMissingType() {
            Discrepancy locationMismatch = buildDiscrepancy(1L, DiscrepancyType.LOCATION_MISMATCH,
                    DiscrepancyStatus.OPEN, 10L, 1L);

            when(discrepancyRepository.findActiveById(1L)).thenReturn(Optional.of(locationMismatch));

            ResolveDiscrepancyRequest request = new ResolveDiscrepancyRequest();
            request.setAction(ResolutionAction.CONFIRM_LOST); // action không hợp lệ cho type này

            assertThatThrownBy(() -> auditService.resolveDiscrepancy(1L, request, 5L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Nên ném AssetNotFoundException khi CONFIRM_LOST nhưng asset không tồn tại")
        void resolve_shouldThrow_whenConfirmLostButAssetNotFound() {
            Discrepancy discrepancy = buildDiscrepancy(1L, DiscrepancyType.MISSING,
                    DiscrepancyStatus.OPEN, 999L, 1L);

            when(discrepancyRepository.findActiveById(1L)).thenReturn(Optional.of(discrepancy));
            when(assetRepository.findActiveById(999L)).thenReturn(Optional.empty());

            ResolveDiscrepancyRequest request = new ResolveDiscrepancyRequest();
            request.setAction(ResolutionAction.CONFIRM_LOST);

            assertThatThrownBy(() -> auditService.resolveDiscrepancy(1L, request, 5L))
                    .isInstanceOf(AssetNotFoundException.class);
        }
    }
}
