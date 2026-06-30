package com.vanh.itam.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.util.List;

/**
 * Generic wrapper cho mọi API response của hệ thống ITAM.
 * Format: { success, data, errors, meta }
 *
 * @param <T> kiểu dữ liệu trả về trong data
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final List<ApiError> errors;
    private final Meta meta;

    private ApiResponse(boolean success, T data, List<ApiError> errors, Meta meta) {
        this.success = success;
        this.data = data;
        this.errors = errors;
        this.meta = meta;
    }

    // ── Success factories ──────────────────────────────────────────────────────

    /** Dùng cho GET chi tiết / POST tạo mới / PUT cập nhật */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, Meta.now());
    }

    /** Dùng cho GET danh sách — kèm pagination */
    public static <T> ApiResponse<T> success(T data, Pagination pagination) {
        return new ApiResponse<>(true, data, null, Meta.withPagination(pagination));
    }

    /** Dùng cho DELETE (204 No Content — không có data) */
    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, null, null, Meta.now());
    }

    // ── Error factories ───────────────────────────────────────────────────────

    public static <T> ApiResponse<T> error(List<ApiError> errors) {
        return new ApiResponse<>(false, null, errors, Meta.now());
    }

    public static <T> ApiResponse<T> error(ApiError error) {
        return new ApiResponse<>(false, null, List.of(error), Meta.now());
    }
}
