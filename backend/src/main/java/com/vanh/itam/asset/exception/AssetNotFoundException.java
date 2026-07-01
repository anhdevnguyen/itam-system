package com.vanh.itam.asset.exception;

import com.vanh.itam.common.exception.ResourceNotFoundException;

public class AssetNotFoundException extends ResourceNotFoundException {

    public AssetNotFoundException(Long id) {
        super("ASSET_NOT_FOUND", "Không tìm thấy thiết bị với ID: " + id);
    }
}
