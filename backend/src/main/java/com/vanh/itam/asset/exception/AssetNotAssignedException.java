package com.vanh.itam.asset.exception;

import com.vanh.itam.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class AssetNotAssignedException extends BusinessException {

    public AssetNotAssignedException(Long id) {
        super("ASSET_NOT_ASSIGNED", "Thiết bị ID " + id + " hiện không được cấp phát cho ai");
    }
}
