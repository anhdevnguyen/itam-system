package com.vanh.itam.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Đại diện 1 lỗi trong mảng errors của ApiResponse.
 * Validation lỗi sẽ có field != null; business/auth error thường field = null.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ApiError {

    /** Mã lỗi UPPER_SNAKE_CASE cho machine-readable (VD: ASSET_NOT_FOUND) */
    private String code;

    /** Tên field vi phạm — chỉ dùng cho Validation errors, null với business errors */
    private String field;

    /** Thông điệp Tiếng Việt hiển thị cho người dùng */
    private String message;
}
