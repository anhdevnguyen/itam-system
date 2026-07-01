package com.vanh.itam.employee.service;

import com.vanh.itam.auth.repository.RefreshTokenRepository;
import com.vanh.itam.common.exception.BusinessException;
import com.vanh.itam.common.exception.ForbiddenException;
import com.vanh.itam.employee.dto.request.ChangePasswordRequest;
import com.vanh.itam.employee.dto.request.CreateEmployeeRequest;
import com.vanh.itam.employee.dto.request.UpdateEmployeeRequest;
import com.vanh.itam.employee.dto.response.EmployeeResponse;
import com.vanh.itam.employee.dto.response.ResetPasswordResponse;
import com.vanh.itam.employee.entity.Branch;
import com.vanh.itam.employee.entity.Department;
import com.vanh.itam.employee.entity.Employee;
import com.vanh.itam.employee.entity.Role;
import com.vanh.itam.employee.exception.BranchNotFoundException;
import com.vanh.itam.employee.exception.DepartmentNotFoundException;
import com.vanh.itam.employee.exception.EmployeeNotFoundException;
import com.vanh.itam.employee.mapper.EmployeeMapper;
import com.vanh.itam.employee.repository.BranchRepository;
import com.vanh.itam.employee.repository.DepartmentRepository;
import com.vanh.itam.employee.repository.EmployeeRepository;
import com.vanh.itam.employee.repository.RoleRepository;
import com.vanh.itam.asset.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeServiceImpl implements EmployeeService {

    // [M2] SecureRandom — CSPRNG bắt buộc cho mật khẩu tạm thời
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";

    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final AssetRepository assetRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmployeeMapper employeeMapper;
    private final PasswordEncoder passwordEncoder;

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    public Page<EmployeeResponse> getAll(Long branchId, Long departmentId, String roleCode, Pageable pageable) {
        return employeeRepository.findAllActive(branchId, departmentId, roleCode, pageable)
                .map(employeeMapper::toResponse);
    }

    @Override
    public EmployeeResponse getById(Long id) {
        return employeeMapper.toResponse(findActiveOrThrow(id));
    }

    @Override
    public EmployeeResponse getMe(Long currentEmployeeId) {
        return employeeMapper.toResponse(findActiveOrThrow(currentEmployeeId));
    }

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * [M1] Đây là method create DUY NHẤT — create() interface đã bị gộp vào đây.
     * Trả kèm temporaryPassword để Controller relay qua response + email.
     */
    @Override
    @Transactional
    public EmployeeCreateResult createWithPassword(CreateEmployeeRequest request, String currentUserRoleCode) {
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new BusinessException("VALIDATION_ERROR", "Vai trò không hợp lệ"));

        if ("IT_STAFF".equals(currentUserRoleCode) && "ADMIN".equals(role.getCode())) {
            throw new ForbiddenException("EMPLOYEE_CANNOT_ASSIGN_ADMIN_ROLE",
                    "IT Staff không được phép tạo hoặc gán vai trò Admin");
        }
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("EMPLOYEE_EMAIL_DUPLICATE", "Email đã được sử dụng bởi nhân viên khác");
        }

        Branch branch = branchRepository.findActiveById(request.getBranchId())
                .orElseThrow(() -> new BranchNotFoundException(request.getBranchId()));

        // [M2] Dùng SecureRandom thông qua generateTemporaryPassword()
        String tempPassword = generateTemporaryPassword();
        Employee employee = buildNewEmployee(request, role, branch, tempPassword);

        if (request.getDepartmentId() != null) {
            Department dept = departmentRepository.findActiveById(request.getDepartmentId())
                    .orElseThrow(() -> new DepartmentNotFoundException(request.getDepartmentId()));
            employee.setDepartment(dept);
        }

        Employee saved = employeeRepository.save(employee);
        log.info("Employee created: email={}, roleCode={}", saved.getEmail(), role.getCode());
        return new EmployeeCreateResult(employeeMapper.toResponse(saved), tempPassword);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public EmployeeResponse update(Long id, UpdateEmployeeRequest request, String currentUserRoleCode) {
        Employee employee = findActiveOrThrow(id);

        if (request.getFullName() != null) employee.setFullName(request.getFullName());

        if (request.getRoleId() != null) {
            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new BusinessException("VALIDATION_ERROR", "Vai trò không hợp lệ"));
            if ("IT_STAFF".equals(currentUserRoleCode) && "ADMIN".equals(role.getCode())) {
                throw new ForbiddenException("EMPLOYEE_CANNOT_ASSIGN_ADMIN_ROLE",
                        "IT Staff không được phép gán vai trò Admin");
            }
            employee.setRole(role);
        }

        if (request.getBranchId() != null) {
            Branch branch = branchRepository.findActiveById(request.getBranchId())
                    .orElseThrow(() -> new BranchNotFoundException(request.getBranchId()));
            employee.setBranch(branch);
        }

        if (request.getDepartmentId() != null) {
            Department dept = departmentRepository.findActiveById(request.getDepartmentId())
                    .orElseThrow(() -> new DepartmentNotFoundException(request.getDepartmentId()));
            employee.setDepartment(dept);
        }

        Employee saved = employeeRepository.save(employee);
        log.info("Employee updated: id={}", id);
        return employeeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public EmployeeResponse updateMe(Long currentEmployeeId, UpdateEmployeeRequest request) {
        Employee employee = findActiveOrThrow(currentEmployeeId);
        if (request.getFullName() != null) employee.setFullName(request.getFullName());
        return employeeMapper.toResponse(employeeRepository.save(employee));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public EmployeeResponse softDelete(Long id) {
        Employee employee = findActiveOrThrow(id);
        employee.softDelete();
        employeeRepository.save(employee);
        refreshTokenRepository.deleteAllByEmployeeId(id);
        log.info("Employee soft-deleted: id={}", id);
        return employeeMapper.toResponse(employee);
    }

    @Override
    public long countAssignedAssets(Long employeeId) {
        return assetRepository.countByAssignedToId(employeeId);
    }

    // ── Password management ───────────────────────────────────────────────────

    @Override
    @Transactional
    public ResetPasswordResponse resetPassword(Long id) {
        Employee employee = findActiveOrThrow(id);
        String tempPassword = generateTemporaryPassword();
        employee.setPasswordHash(passwordEncoder.encode(tempPassword));
        employee.setMustChangePassword(true);
        employeeRepository.save(employee);
        refreshTokenRepository.deleteAllByEmployeeId(id);
        log.info("Password reset by admin: employeeId={}", id);
        return new ResetPasswordResponse(tempPassword);
    }

    @Override
    @Transactional
    public void changePassword(Long currentEmployeeId, ChangePasswordRequest request,
                               String currentRefreshTokenHash) {
        Employee employee = findActiveOrThrow(currentEmployeeId);

        // [M3] Đổi error code → AUTH_OLD_PASSWORD_INCORRECT (khớp docs 09-ERROR-CODES.md)
        if (!passwordEncoder.matches(request.getOldPassword(), employee.getPasswordHash())) {
            throw new BusinessException("AUTH_OLD_PASSWORD_INCORRECT",
                    HttpStatus.UNPROCESSABLE_ENTITY, "Mật khẩu cũ không đúng");
        }
        if (passwordEncoder.matches(request.getNewPassword(), employee.getPasswordHash())) {
            throw new BusinessException("AUTH_PASSWORD_SAME_AS_OLD",
                    HttpStatus.UNPROCESSABLE_ENTITY, "Mật khẩu mới không được trùng mật khẩu cũ");
        }

        employee.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        employee.setMustChangePassword(false);
        employeeRepository.save(employee);

        // [C3] Revoke toàn bộ TRỪ phiên hiện tại (nếu có hash)
        if (currentRefreshTokenHash != null) {
            refreshTokenRepository.deleteAllByEmployeeIdExcept(currentEmployeeId, currentRefreshTokenHash);
        } else {
            refreshTokenRepository.deleteAllByEmployeeId(currentEmployeeId);
        }

        log.info("Password changed: employeeId={}", currentEmployeeId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Employee findActiveOrThrow(Long id) {
        return employeeRepository.findActiveById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    private Employee buildNewEmployee(CreateEmployeeRequest request, Role role,
                                      Branch branch, String rawPassword) {
        Employee e = new Employee();
        e.setEmail(request.getEmail());
        e.setFullName(request.getFullName());
        e.setPasswordHash(passwordEncoder.encode(rawPassword));
        e.setRole(role);
        e.setBranch(branch);
        e.setMustChangePassword(true);
        return e;
    }

    /**
     * [M2] Sinh mật khẩu tạm >= 8 ký tự, đủ hoa/thường/số, dùng SecureRandom.
     * [L4] Không dùng RandomStringUtils deprecated — implement trực tiếp.
     */
    private String generateTemporaryPassword() {
        char[] all = new char[8];
        // Đảm bảo ít nhất 1 hoa, 1 thường, 1 số
        all[0] = UPPER.charAt(SECURE_RANDOM.nextInt(UPPER.length()));
        all[1] = UPPER.charAt(SECURE_RANDOM.nextInt(UPPER.length()));
        all[2] = LOWER.charAt(SECURE_RANDOM.nextInt(LOWER.length()));
        all[3] = LOWER.charAt(SECURE_RANDOM.nextInt(LOWER.length()));
        all[4] = LOWER.charAt(SECURE_RANDOM.nextInt(LOWER.length()));
        all[5] = DIGITS.charAt(SECURE_RANDOM.nextInt(DIGITS.length()));
        all[6] = DIGITS.charAt(SECURE_RANDOM.nextInt(DIGITS.length()));
        all[7] = DIGITS.charAt(SECURE_RANDOM.nextInt(DIGITS.length()));
        // Fisher-Yates shuffle với SecureRandom
        for (int i = 7; i > 0; i--) {
            int j = SECURE_RANDOM.nextInt(i + 1);
            char tmp = all[i]; all[i] = all[j]; all[j] = tmp;
        }
        return new String(all);
    }
}
