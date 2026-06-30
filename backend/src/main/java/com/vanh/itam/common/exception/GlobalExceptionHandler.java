package com.vanh.itam.common.exception;

import com.vanh.itam.common.response.ApiError;
import com.vanh.itam.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Tập trung xử lý mọi exception — đảm bảo format ApiResponse nhất quán toàn hệ thống.
 * Không có Controller nào tự try-catch để trả ResponseEntity riêng lẻ.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Xử lý mọi BaseException (BusinessException, ResourceNotFoundException,
     * ValidationException, UnauthorizedException, ForbiddenException).
     * Log ở WARN — lỗi nghiệp vụ có chủ đích, hệ thống vẫn hoạt động đúng.
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException ex) {
        log.warn("Business/Client error: code={}, status={}, message={}",
                ex.getErrorCode(), ex.getHttpStatus(), ex.getMessage());

        ApiError error = new ApiError(ex.getErrorCode(), null, ex.getMessage());
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(ApiResponse.error(error));
    }

    /**
     * Xử lý lỗi Jakarta Bean Validation — trả TẤT CẢ lỗi field trong 1 response.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        List<ApiError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError("VALIDATION_ERROR", fe.getField(), fe.getDefaultMessage()))
                .toList();

        // Cũng xử lý class-level constraint violations
        List<ApiError> globalErrors = ex.getBindingResult().getGlobalErrors().stream()
                .map(oe -> new ApiError("VALIDATION_ERROR", null, oe.getDefaultMessage()))
                .toList();

        List<ApiError> allErrors = new java.util.ArrayList<>(errors);
        allErrors.addAll(globalErrors);

        log.warn("Validation failed: fieldsCount={}, fields={}",
                allErrors.size(),
                ex.getBindingResult().getFieldErrors().stream().map(FieldError::getField).toList());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(allErrors));
    }

    /**
     * Xử lý AccessDeniedException từ Spring Security (@PreAuthorize fail).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        ApiError error = new ApiError("AUTH_FORBIDDEN", null, "Bạn không có quyền thực hiện hành động này");
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(error));
    }

    /**
     * Catch-all cho mọi exception không xác định.
     * Log ở ERROR — cần điều tra.
     * KHÔNG bao giờ lộ stack trace hay thông tin kỹ thuật ra response.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex) {
        log.error("Unexpected system error", ex);
        ApiError error = new ApiError("INTERNAL_SERVER_ERROR", null, "Đã có lỗi xảy ra, vui lòng thử lại sau");
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(error));
    }
}
