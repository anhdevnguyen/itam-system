package com.vanh.itam.maintenance.service;

import com.vanh.itam.asset.entity.Asset;
import com.vanh.itam.asset.entity.AssetStatus;
import com.vanh.itam.asset.exception.AssetNotFoundException;
import com.vanh.itam.asset.repository.AssetRepository;
import com.vanh.itam.maintenance.dto.request.CreateMaintenanceRequest;
import com.vanh.itam.maintenance.dto.request.UpdateMaintenanceRequest;
import com.vanh.itam.maintenance.dto.response.MaintenanceResponse;
import com.vanh.itam.maintenance.entity.MaintenanceRecord;
import com.vanh.itam.maintenance.entity.MaintenanceStatus;
import com.vanh.itam.maintenance.entity.MaintenanceType;
import com.vanh.itam.maintenance.exception.MaintenanceRecordNotFoundException;
import com.vanh.itam.maintenance.mapper.MaintenanceMapper;
import com.vanh.itam.maintenance.repository.MaintenanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceServiceImpl implements MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final AssetRepository assetRepository;
    private final MaintenanceMapper maintenanceMapper;

    @Override
    public Page<MaintenanceResponse> getAll(Long assetId, MaintenanceStatus status,
                                             MaintenanceType type, Pageable pageable) {
        return maintenanceRepository.findAllActive(assetId, status, type, pageable)
                .map(maintenanceMapper::toResponse);
    }

    @Override
    public MaintenanceResponse getById(Long id) {
        return maintenanceMapper.toResponse(findActiveOrThrow(id));
    }

    @Override
    @Transactional
    public MaintenanceResponse create(CreateMaintenanceRequest request) {
        Asset asset = assetRepository.findActiveById(request.getAssetId())
                .orElseThrow(() -> new AssetNotFoundException(request.getAssetId()));

        MaintenanceRecord record = maintenanceMapper.toEntity(request);
        record.setAsset(asset);
        record.setStatus(MaintenanceStatus.SCHEDULED);

        MaintenanceRecord saved = maintenanceRepository.save(record);
        log.info("MaintenanceRecord created: id={}, assetId={}, type={}",
                saved.getId(), asset.getId(), request.getType());
        return maintenanceMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public MaintenanceResponse update(Long id, UpdateMaintenanceRequest request) {
        MaintenanceRecord record = findActiveOrThrow(id);
        MaintenanceStatus oldStatus = record.getStatus();
        MaintenanceStatus newStatus = request.getStatus();

        record.setStatus(newStatus);
        if (request.getDescription() != null) record.setDescription(request.getDescription());
        if (request.getScheduledDate() != null) record.setScheduledDate(request.getScheduledDate());

        // Tự động set completedDate khi COMPLETED
        if (newStatus == MaintenanceStatus.COMPLETED && record.getCompletedDate() == null) {
            record.setCompletedDate(request.getCompletedDate() != null
                    ? request.getCompletedDate() : LocalDate.now());
        }

        maintenanceRepository.save(record);

        // Đồng bộ asset.status theo business rule (docs 07 mục 3.1)
        syncAssetStatus(record.getAsset(), oldStatus, newStatus);

        log.info("MaintenanceRecord updated: id={}, newStatus={}, assetId={}",
                id, newStatus, record.getAsset().getId());
        return maintenanceMapper.toResponse(record);
    }

    @Override
    @Transactional
    public void softDelete(Long id) {
        MaintenanceRecord record = findActiveOrThrow(id);
        record.softDelete();
        maintenanceRepository.save(record);
        log.info("MaintenanceRecord soft-deleted: id={}", id);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private MaintenanceRecord findActiveOrThrow(Long id) {
        return maintenanceRepository.findActiveById(id)
                .orElseThrow(() -> new MaintenanceRecordNotFoundException(id));
    }

    /**
     * Đồng bộ asset.status theo trạng thái bảo trì (docs 07 mục 3.1).
     * SCHEDULED  → không đổi asset.status
     * IN_PROGRESS → asset.status = IN_MAINTENANCE (giữ nguyên assigned_to)
     * COMPLETED / CANCELLED → khôi phục asset.status:
     *   - ASSIGNED nếu assigned_to != null
     *   - AVAILABLE nếu assigned_to == null
     */
    private void syncAssetStatus(Asset asset, MaintenanceStatus oldStatus, MaintenanceStatus newStatus) {
        if (newStatus == MaintenanceStatus.IN_PROGRESS) {
            asset.setStatus(AssetStatus.IN_MAINTENANCE);
            assetRepository.save(asset);
            log.info("Asset status set to IN_MAINTENANCE: assetId={}", asset.getId());

        } else if (newStatus == MaintenanceStatus.COMPLETED || newStatus == MaintenanceStatus.CANCELLED) {
            // Chỉ khôi phục nếu trước đó đã IN_PROGRESS (đang bảo trì thực sự)
            if (oldStatus == MaintenanceStatus.IN_PROGRESS
                    || asset.getStatus() == AssetStatus.IN_MAINTENANCE) {
                AssetStatus restored = asset.getAssignedTo() != null
                        ? AssetStatus.ASSIGNED : AssetStatus.AVAILABLE;
                asset.setStatus(restored);
                assetRepository.save(asset);
                log.info("Asset status restored to {} after maintenance: assetId={}",
                        restored, asset.getId());
            }
        }
    }
}
