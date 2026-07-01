package com.vanh.itam.asset.service;

import com.cloudinary.Cloudinary;
import com.vanh.itam.asset.dto.request.ForceReturnRequest;
import com.vanh.itam.asset.dto.response.AssetResponse;
import com.vanh.itam.asset.entity.Asset;
import com.vanh.itam.asset.entity.AssetAssignmentHistory;
import com.vanh.itam.asset.entity.AssetStatus;
import com.vanh.itam.asset.exception.AssetNotFoundException;
import com.vanh.itam.asset.mapper.AssetMapper;
import com.vanh.itam.asset.repository.AssetAssignmentHistoryRepository;
import com.vanh.itam.asset.repository.AssetImageRepository;
import com.vanh.itam.asset.repository.AssetRepository;
import com.vanh.itam.asset.repository.CategoryRepository;
import com.vanh.itam.audit.entity.NotificationType;
import com.vanh.itam.audit.service.NotificationService;
import com.vanh.itam.common.exception.BusinessException;
import com.vanh.itam.common.util.QrCodeGenerator;
import com.vanh.itam.employee.entity.Branch;
import com.vanh.itam.employee.entity.Employee;
import com.vanh.itam.employee.repository.BranchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho AssetServiceImpl.
 * Kiểm tra: generateAssetCode (format + retry), forceReturn (side-effect đúng).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssetServiceImpl — Unit Tests")
class AssetServiceImplTest {

    @Mock private AssetRepository assetRepository;
    @Mock private AssetImageRepository assetImageRepository;
    @Mock private AssetAssignmentHistoryRepository historyRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private AssetMapper assetMapper;
    @Mock private QrCodeGenerator qrCodeGenerator;
    @Mock private Cloudinary cloudinary;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private AssetServiceImpl assetService;

    // ── Helpers ────────────────────────────────────────────────────────────

    private Asset buildAssignedAsset(Long assetId, Long holderId) {
        Asset asset = new Asset();
        asset.setId(assetId);
        asset.setCode("HN-LAP-000" + assetId);
        asset.setStatus(AssetStatus.ASSIGNED);
        Branch branch = new Branch();
        branch.setId(1L);
        asset.setBranch(branch);

        Employee holder = new Employee();
        holder.setId(holderId);
        holder.setFullName("Holder " + holderId);
        asset.setAssignedTo(holder);

        return asset;
    }

    // ── generateAssetCode (via create) ─────────────────────────────────────

    @Nested
    @DisplayName("generateAssetCode()")
    class GenerateAssetCodeTests {

        @Test
        @DisplayName("Nên sinh code đúng format: BRANCH-CAT-NNNN")
        void generateAssetCode_shouldFollowCorrectFormat() {
            Long branchId = 1L;
            Long categoryId = 2L;

            // Sequence tiếp theo = 7 → code = HN-LAP-0007
            when(assetRepository.findNextSequence(branchId, categoryId)).thenReturn(7);
            when(assetRepository.existsByCode("HN-LAP-0007")).thenReturn(false);

            // Setup category + branch mock
            com.vanh.itam.asset.entity.Category cat = new com.vanh.itam.asset.entity.Category();
            cat.setId(categoryId);
            cat.setCode("LAP");
            cat.setName("Laptop");

            Branch branch = new Branch();
            branch.setId(branchId);
            branch.setCode("HN");
            branch.setName("Hà Nội");

            com.vanh.itam.asset.dto.request.CreateAssetRequest req =
                    new com.vanh.itam.asset.dto.request.CreateAssetRequest();
            req.setBranchId(branchId);
            req.setCategoryId(categoryId);
            req.setName("Test Laptop");
            req.setPurchaseDate(java.time.LocalDate.now());
            req.setValue(java.math.BigDecimal.valueOf(10000000));

            Asset savedAsset = new Asset();
            savedAsset.setId(1L);
            savedAsset.setCode("HN-LAP-0007");
            AssetResponse mockResponse = mock(AssetResponse.class);

            when(categoryRepository.findActiveById(categoryId)).thenReturn(Optional.of(cat));
            when(branchRepository.findActiveById(branchId)).thenReturn(Optional.of(branch));
            when(assetMapper.toEntity(any())).thenReturn(savedAsset);
            when(assetRepository.save(any())).thenReturn(savedAsset);
            when(assetMapper.toResponse(any())).thenReturn(mockResponse);

            AssetResponse result = assetService.create(req);

            assertThat(result).isEqualTo(mockResponse);
            // Xác nhận code được set theo format đúng
            verify(assetRepository).findNextSequence(branchId, categoryId);
            verify(assetRepository).existsByCode("HN-LAP-0007");
        }

        @Test
        @DisplayName("Nên retry khi code đã tồn tại, dùng sequence tiếp theo")
        void generateAssetCode_shouldRetry_whenCodeCollision() {
            Long branchId = 1L;
            Long categoryId = 2L;

            com.vanh.itam.asset.entity.Category cat = new com.vanh.itam.asset.entity.Category();
            cat.setId(categoryId);
            cat.setCode("LAP");
            cat.setName("Laptop");

            Branch branch = new Branch();
            branch.setId(branchId);
            branch.setCode("HN");
            branch.setName("Hà Nội");

            com.vanh.itam.asset.dto.request.CreateAssetRequest req =
                    new com.vanh.itam.asset.dto.request.CreateAssetRequest();
            req.setBranchId(branchId);
            req.setCategoryId(categoryId);
            req.setName("Test Laptop");
            req.setPurchaseDate(java.time.LocalDate.now());
            req.setValue(java.math.BigDecimal.valueOf(10000000));

            Asset savedAsset = new Asset();
            savedAsset.setId(1L);
            savedAsset.setCode("HN-LAP-0002");
            AssetResponse mockResponse = mock(AssetResponse.class);

            when(categoryRepository.findActiveById(categoryId)).thenReturn(Optional.of(cat));
            when(branchRepository.findActiveById(branchId)).thenReturn(Optional.of(branch));
            when(assetMapper.toEntity(any())).thenReturn(savedAsset);
            when(assetRepository.save(any())).thenReturn(savedAsset);
            when(assetMapper.toResponse(any())).thenReturn(mockResponse);

            // Lần 1: sequence=1 → code HN-LAP-0001 đã tồn tại
            // Lần 2: sequence=2 → code HN-LAP-0002 chưa tồn tại → thành công
            when(assetRepository.findNextSequence(branchId, categoryId))
                    .thenReturn(1, 2);
            when(assetRepository.existsByCode("HN-LAP-0001")).thenReturn(true);
            when(assetRepository.existsByCode("HN-LAP-0002")).thenReturn(false);

            assetService.create(req);

            // Phải gọi findNextSequence 2 lần do collision lần đầu
            verify(assetRepository, times(2)).findNextSequence(branchId, categoryId);
        }

        @Test
        @DisplayName("Nên ném BusinessException sau MAX_RETRIES lần collision liên tiếp")
        void generateAssetCode_shouldThrow_afterMaxRetries() {
            Long branchId = 1L;
            Long categoryId = 2L;

            com.vanh.itam.asset.entity.Category cat = new com.vanh.itam.asset.entity.Category();
            cat.setId(categoryId);
            cat.setCode("LAP");
            cat.setName("Laptop");

            Branch branch = new Branch();
            branch.setId(branchId);
            branch.setCode("HN");
            branch.setName("Hà Nội");

            com.vanh.itam.asset.dto.request.CreateAssetRequest req =
                    new com.vanh.itam.asset.dto.request.CreateAssetRequest();
            req.setBranchId(branchId);
            req.setCategoryId(categoryId);
            req.setName("Test Laptop");
            req.setPurchaseDate(java.time.LocalDate.now());
            req.setValue(java.math.BigDecimal.valueOf(10000000));

            when(categoryRepository.findActiveById(categoryId)).thenReturn(Optional.of(cat));
            when(branchRepository.findActiveById(branchId)).thenReturn(Optional.of(branch));
            when(assetMapper.toEntity(any())).thenReturn(new Asset());
            // Tất cả 5 lần đều collision
            when(assetRepository.findNextSequence(branchId, categoryId))
                    .thenReturn(1, 2, 3, 4, 5);
            when(assetRepository.existsByCode(anyString())).thenReturn(true);

            assertThatThrownBy(() -> assetService.create(req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("mã thiết bị");
        }
    }

    // ── forceReturn ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("forceReturn()")
    class ForceReturnTests {

        @Test
        @DisplayName("Nên force-return thành công: đóng history, set asset AVAILABLE, notify holder cũ")
        void forceReturn_shouldSucceed_andNotifyPreviousHolder() {
            Long assetId = 1L;
            Long holderId = 5L;
            Long itStaffId = 20L;

            Asset asset = buildAssignedAsset(assetId, holderId);
            AssetResponse mockResponse = mock(AssetResponse.class);
            AssetAssignmentHistory openHistory = new AssetAssignmentHistory();

            when(assetRepository.findActiveById(assetId)).thenReturn(Optional.of(asset));
            when(historyRepository.findOpenAssignment(assetId)).thenReturn(Optional.of(openHistory));
            when(assetRepository.save(any())).thenReturn(asset);
            when(assetMapper.toResponse(any())).thenReturn(mockResponse);

            ForceReturnRequest req = new ForceReturnRequest();
            req.setReason("Nhân viên nghỉ việc");

            AssetResponse result = assetService.forceReturn(assetId, req, itStaffId);

            assertThat(result).isEqualTo(mockResponse);
            assertThat(asset.getStatus()).isEqualTo(AssetStatus.AVAILABLE);
            assertThat(asset.getAssignedTo()).isNull();
            assertThat(openHistory.getReturnedAt()).isNotNull();

            verify(historyRepository).save(openHistory);
            verify(notificationService).notify(eq(holderId),
                    eq(NotificationType.ASSET_FORCE_RETURNED), anyString());
        }

        @Test
        @DisplayName("Nên ném BusinessException khi force-return asset không được cấp phát cho ai")
        void forceReturn_shouldThrow_whenAssetNotAssigned() {
            Long assetId = 1L;
            Asset asset = new Asset();
            asset.setId(assetId);
            asset.setCode("HN-LAP-0001");
            asset.setStatus(AssetStatus.AVAILABLE);
            asset.setAssignedTo(null); // không được cấp phát

            when(assetRepository.findActiveById(assetId)).thenReturn(Optional.of(asset));

            ForceReturnRequest req = new ForceReturnRequest();
            req.setReason("Test reason");

            assertThatThrownBy(() -> assetService.forceReturn(assetId, req, 20L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("không được cấp phát");
        }

        @Test
        @DisplayName("Nên ném AssetNotFoundException khi asset không tồn tại")
        void forceReturn_shouldThrow_whenAssetNotFound() {
            when(assetRepository.findActiveById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> assetService.forceReturn(999L, new ForceReturnRequest(), 20L))
                    .isInstanceOf(AssetNotFoundException.class);
        }

        @Test
        @DisplayName("Nên vẫn hoạt động khi không có open assignment history")
        void forceReturn_shouldSucceed_evenWithNoOpenHistory() {
            Long assetId = 1L;
            Long holderId = 5L;

            Asset asset = buildAssignedAsset(assetId, holderId);
            AssetResponse mockResponse = mock(AssetResponse.class);

            when(assetRepository.findActiveById(assetId)).thenReturn(Optional.of(asset));
            when(historyRepository.findOpenAssignment(assetId)).thenReturn(Optional.empty()); // không có history
            when(assetRepository.save(any())).thenReturn(asset);
            when(assetMapper.toResponse(any())).thenReturn(mockResponse);

            ForceReturnRequest req = new ForceReturnRequest();
            req.setReason("Admin test");

            AssetResponse result = assetService.forceReturn(assetId, req, 20L);

            assertThat(result).isEqualTo(mockResponse);
            assertThat(asset.getStatus()).isEqualTo(AssetStatus.AVAILABLE);
            verify(historyRepository, never()).save(any()); // không save history vì không có
        }
    }
}
