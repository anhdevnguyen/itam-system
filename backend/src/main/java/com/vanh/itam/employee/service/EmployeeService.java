package com.vanh.itam.employee.service;

import com.vanh.itam.employee.dto.request.ChangePasswordRequest;
import com.vanh.itam.employee.dto.request.CreateEmployeeRequest;
import com.vanh.itam.employee.dto.request.UpdateEmployeeRequest;
import com.vanh.itam.employee.dto.response.EmployeeResponse;
import com.vanh.itam.employee.dto.response.ResetPasswordResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmployeeService {
    Page<EmployeeResponse> getAll(Long branchId, Long departmentId, String roleCode, Pageable pageable);
    EmployeeResponse getById(Long id);
    EmployeeResponse getMe(Long currentEmployeeId);

    /**
     * Tạo employee mới, trả kèm mật khẩu tạm — Controller sẽ relay cho Admin.
     */
    EmployeeCreateResult createWithPassword(CreateEmployeeRequest request, String currentUserRoleCode);

    EmployeeResponse update(Long id, UpdateEmployeeRequest request, String currentUserRoleCode);
    EmployeeResponse updateMe(Long currentEmployeeId, UpdateEmployeeRequest request);

    /** Soft-delete employee — không chặn, nhưng trả cảnh báo nếu đang giữ thiết bị */
    EmployeeResponse softDelete(Long id);

    /** Đếm số asset employee đang giữ — dùng để gắn warning khi soft-delete */
    long countAssignedAssets(Long employeeId);

    ResetPasswordResponse resetPassword(Long id);

    /** Đổi mật khẩu — set mustChangePassword=false, revoke session cũ trừ phiên hiện tại */
    void changePassword(Long currentEmployeeId, ChangePasswordRequest request, String currentRefreshTokenHash);

    /** Result record trả cả employee response lẫn mật khẩu tạm thời */
    record EmployeeCreateResult(EmployeeResponse employee, String temporaryPassword) {}
}
