package com.vanh.itam.asset.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.vanh.itam.asset.dto.request.CreateAssetRequest;
import com.vanh.itam.asset.dto.request.ForceReturnRequest;
import com.vanh.itam.asset.dto.request.UpdateAssetRequest;
import com.vanh.itam.asset.dto.response.AssetAssignmentHistoryResponse;
import com.vanh.itam.asset.dto.response.AssetImageResponse;
import com.vanh.itam.asset.dto.response.AssetResponse;
import com.vanh.itam.asset.entity.*;
import com.vanh.itam.asset.exception.AssetNotFoundException;
import com.vanh.itam.asset.exception.AssetNotAvailableException;
import com.vanh.itam.asset.mapper.AssetMapper;
import com.vanh.itam.asset.repository.*;
import com.vanh.itam.audit.entity.NotificationType;
import com.vanh.itam.audit.service.NotificationService;
import com.vanh.itam.common.exception.BusinessException;
import com.vanh.itam.common.util.QrCodeGenerator;
import com.vanh.itam.employee.entity.Branch;
import com.vanh.itam.employee.entity.Employee;
import com.vanh.itam.employee.exception.BranchNotFoundException;
import com.vanh.itam.asset.exception.CategoryNotFoundException;
import com.vanh.itam.employee.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetServiceImpl implements AssetService {

    private static final int MAX_IMAGES_PER_ASSET = 5;
    private static final int MAX_CODE_GEN_RETRIES = 5;

    private final AssetRepository assetRepository;
    private final AssetImageRepository assetImageRepository;
    private final AssetAssignmentHistoryRepository historyRepository;
    private final CategoryRepository categoryRepository;
    private final BranchRepository branchRepository;
    private final AssetMapper assetMapper;
    private final QrCodeGenerator qrCodeGenerator;
    private final Cloudinary cloudinary;
    private final NotificationService notificationService;

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    public Page<AssetResponse> getAll(Long branchId, AssetStatus status, Long categoryId,
                                       Long assignedToId, Pageable pageable) {
        return assetRepository.findAllActive(branchId, status, categoryId, assignedToId, pageable)
                .map(assetMapper::toResponse);
    }

    @Override
    public AssetResponse getById(Long id) {
        return assetMapper.toResponse(findActiveOrThrow(id));
    }

    @Override
    public Page<AssetAssignmentHistoryResponse> getAssignmentHistory(Long id, Pageable pageable) {
        findActiveOrThrow(id); // validate asset tồn tại
        return historyRepository.findByAssetId(id, pageable)
                .map(assetMapper::toHistoryResponse);
    }

    @Override
    public List<AssetImageResponse> getImages(Long assetId) {
        findActiveOrThrow(assetId);
        return assetImageRepository.findActiveByAssetId(assetId).stream()
                .map(assetMapper::toImageResponse)
                .toList();
    }

    // ── Create / Update / Delete ──────────────────────────────────────────────

    @Override
    @Transactional
    public AssetResponse create(CreateAssetRequest request) {
        Category category = categoryRepository.findActiveById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(request.getCategoryId()));
        Branch branch = branchRepository.findActiveById(request.getBranchId())
                .orElseThrow(() -> new BranchNotFoundException(request.getBranchId()));

        Asset asset = assetMapper.toEntity(request);
        asset.setCategory(category);
        asset.setBranch(branch);
        asset.setStatus(AssetStatus.AVAILABLE);
        asset.setCode(generateAssetCode(branch.getCode(), category.getCode(),
                request.getBranchId(), request.getCategoryId()));

        Asset saved = assetRepository.save(asset);
        log.info("Asset created: code={}, branchId={}, categoryId={}",
                saved.getCode(), branch.getId(), category.getId());
        return assetMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AssetResponse update(Long id, UpdateAssetRequest request) {
        Asset asset = findActiveOrThrow(id);
        asset.setName(request.getName());
        asset.setPurchaseDate(request.getPurchaseDate());
        asset.setValue(request.getValue());

        if (request.getStatus() != null) {
            asset.setStatus(request.getStatus());
        }
        if (request.getCategoryId() != null) {
            Category cat = categoryRepository.findActiveById(request.getCategoryId())
                    .orElseThrow(() -> new CategoryNotFoundException(request.getCategoryId()));
            asset.setCategory(cat);
        }

        Asset saved = assetRepository.save(asset);
        log.info("Asset updated: id={}", id);
        return assetMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void softDelete(Long id) {
        Asset asset = findActiveOrThrow(id);
        asset.softDelete();
        assetRepository.save(asset);
        log.info("Asset soft-deleted: id={}", id);
    }

    @Override
    @Transactional
    public AssetResponse restore(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new AssetNotFoundException(id));
        asset.restore();
        return assetMapper.toResponse(assetRepository.save(asset));
    }

    // ── QR Code ───────────────────────────────────────────────────────────────

    @Override
    public byte[] getQrCode(Long id) {
        Asset asset = findActiveOrThrow(id);
        try {
            return qrCodeGenerator.generate(asset.getCode(), 300, 300);
        } catch (Exception e) {
            log.error("Failed to generate QR code: assetId={}", id, e);
            throw new BusinessException("INTERNAL_SERVER_ERROR", "Không thể tạo mã QR, vui lòng thử lại");
        }
    }

    // ── Force Return ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AssetResponse forceReturn(Long assetId, ForceReturnRequest request, Long itStaffId) {
        Asset asset = findActiveOrThrow(assetId);

        if (asset.getAssignedTo() == null) {
            throw new BusinessException("ASSET_NOT_ASSIGNED",
                    "Thiết bị hiện không được cấp phát cho ai");
        }

        Long previousHolderId = asset.getAssignedTo().getId();

        // Đóng dòng lịch sử đang mở
        historyRepository.findOpenAssignment(assetId).ifPresent(history -> {
            history.setReturnedAt(Instant.now());
            historyRepository.save(history);
        });

        asset.setStatus(AssetStatus.AVAILABLE);
        asset.setAssignedTo(null);
        assetRepository.save(asset);

        log.info("Force-return executed: assetId={}, previousHolder={}, byItStaff={}, reason={}",
                assetId, previousHolderId, itStaffId, request.getReason());

        // Thông báo cho nhân viên trước đó đang giữ thiết bị
        notificationService.notify(previousHolderId, NotificationType.ASSET_FORCE_RETURNED,
                "Thiết bị " + asset.getCode() + " đã được IT thu hồi: " + request.getReason());

        return assetMapper.toResponse(asset);
    }

    // ── Image Upload ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AssetImageResponse uploadImage(Long assetId, MultipartFile file) {
        Asset asset = findActiveOrThrow(assetId);

        long imageCount = assetImageRepository.countActiveByAssetId(assetId);
        if (imageCount >= MAX_IMAGES_PER_ASSET) {
            throw new BusinessException("ASSET_IMAGE_LIMIT_EXCEEDED",
                    "Mỗi thiết bị chỉ được tối đa " + MAX_IMAGES_PER_ASSET + " ảnh");
        }

        String url;
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("folder", "itam/assets/" + assetId));
            url = (String) result.get("secure_url");
        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary: assetId={}", assetId, e);
            throw new BusinessException("ASSET_IMAGE_UPLOAD_FAILED", "Tải ảnh thất bại, vui lòng thử lại");
        }

        AssetImage image = new AssetImage();
        image.setAsset(asset);
        image.setUrl(url);
        AssetImage saved = assetImageRepository.save(image);

        log.info("Asset image uploaded: assetId={}, imageId={}", assetId, saved.getId());
        return assetMapper.toImageResponse(saved);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Asset findActiveOrThrow(Long id) {
        return assetRepository.findActiveById(id)
                .orElseThrow(() -> new AssetNotFoundException(id));
    }

    /**
     * Sinh asset code theo format: <BRANCH_CODE>-<CATEGORY_CODE>-<4_DIGITS_SEQUENCE>
     * Retry tối đa MAX_CODE_GEN_RETRIES lần để tránh race condition.
     */
    private String generateAssetCode(String branchCode, String categoryCode,
                                      Long branchId, Long categoryId) {
        for (int attempt = 1; attempt <= MAX_CODE_GEN_RETRIES; attempt++) {
            int seq = assetRepository.findNextSequence(branchId, categoryId);
            String code = "%s-%s-%04d".formatted(branchCode, categoryCode, seq);
            if (!assetRepository.existsByCode(code)) {
                return code;
            }
            log.warn("Asset code collision on attempt {}: code={}", attempt, code);
        }
        throw new BusinessException("ASSET_CODE_GENERATION_CONFLICT",
                "Không thể sinh mã thiết bị, vui lòng thử lại");
    }
}
