package com.vanh.itam.asset.service;

import com.vanh.itam.asset.dto.request.CreateAssetRequest;
import com.vanh.itam.asset.dto.request.ForceReturnRequest;
import com.vanh.itam.asset.dto.request.UpdateAssetRequest;
import com.vanh.itam.asset.dto.response.AssetAssignmentHistoryResponse;
import com.vanh.itam.asset.dto.response.AssetImageResponse;
import com.vanh.itam.asset.dto.response.AssetResponse;
import com.vanh.itam.asset.entity.AssetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AssetService {
    Page<AssetResponse> getAll(Long branchId, AssetStatus status, Long categoryId, Long assignedToId, Pageable pageable);
    AssetResponse getById(Long id);
    AssetResponse create(CreateAssetRequest request);
    AssetResponse update(Long id, UpdateAssetRequest request);
    void softDelete(Long id);
    AssetResponse restore(Long id);
    /** Lấy QR code PNG bytes */
    byte[] getQrCode(Long id);
    Page<AssetAssignmentHistoryResponse> getAssignmentHistory(Long id, Pageable pageable);
    AssetResponse forceReturn(Long assetId, ForceReturnRequest request, Long itStaffId);
    AssetImageResponse uploadImage(Long assetId, MultipartFile file);
    List<AssetImageResponse> getImages(Long assetId);
}
