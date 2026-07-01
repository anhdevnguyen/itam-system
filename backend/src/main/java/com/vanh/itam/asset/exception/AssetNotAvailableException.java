package com.vanh.itam.asset.exception;

import com.vanh.itam.common.exception.BusinessException;

public class AssetNotAvailableException extends BusinessException {

    public AssetNotAvailableException(Long id) {
        super("ASSET_NOT_AVAILABLE",
                "Thiết bị ID " + id + " hiện không khả dụng (đã giữ chỗ hoặc không ở trạng thái AVAILABLE)");
    }
}
