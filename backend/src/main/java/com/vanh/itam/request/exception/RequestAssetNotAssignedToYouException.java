package com.vanh.itam.request.exception;

import com.vanh.itam.common.exception.ForbiddenException;

public class RequestAssetNotAssignedToYouException extends ForbiddenException {

    public RequestAssetNotAssignedToYouException() {
        super("REQUEST_ASSET_NOT_ASSIGNED_TO_YOU",
                "Bạn không thể yêu cầu trả thiết bị không phải do mình đang giữ");
    }
}
