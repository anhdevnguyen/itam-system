package com.vanh.itam.request.service;

import com.vanh.itam.asset.entity.Asset;
import com.vanh.itam.asset.entity.AssetAssignmentHistory;
import com.vanh.itam.asset.entity.AssetStatus;
import com.vanh.itam.asset.exception.AssetNotFoundException;
import com.vanh.itam.asset.exception.AssetNotAvailableException;
import com.vanh.itam.asset.repository.AssetAssignmentHistoryRepository;
import com.vanh.itam.asset.repository.AssetRepository;
import com.vanh.itam.audit.entity.NotificationType;
import com.vanh.itam.audit.service.NotificationService;
import com.vanh.itam.common.exception.BusinessException;
import com.vanh.itam.common.exception.ForbiddenException;
import com.vanh.itam.employee.entity.Department;
import com.vanh.itam.employee.entity.Employee;
import com.vanh.itam.employee.exception.DepartmentNotFoundException;
import com.vanh.itam.employee.exception.EmployeeNotFoundException;
import com.vanh.itam.employee.repository.DepartmentRepository;
import com.vanh.itam.employee.repository.EmployeeRepository;
import com.vanh.itam.request.dto.request.CreateRequestRequest;
import com.vanh.itam.request.dto.request.RejectRequestRequest;
import com.vanh.itam.request.dto.response.RequestResponse;
import com.vanh.itam.request.entity.Request;
import com.vanh.itam.request.entity.RequestStatus;
import com.vanh.itam.request.entity.RequestType;
import com.vanh.itam.request.exception.RequestAlreadyProcessedException;
import com.vanh.itam.request.exception.RequestNotFoundException;
import com.vanh.itam.request.mapper.RequestMapper;
import com.vanh.itam.request.repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final AssetRepository assetRepository;
    private final AssetAssignmentHistoryRepository historyRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final RequestMapper requestMapper;
    private final NotificationService notificationService;

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    public Page<RequestResponse> getAll(RequestStatus status, Long employeeId,
                                         Long branchId, Pageable pageable) {
        return requestRepository.findAllActive(status, employeeId, branchId, pageable)
                .map(requestMapper::toResponse);
    }

    @Override
    public RequestResponse getById(Long id) {
        return requestMapper.toResponse(findActiveOrThrow(id));
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public RequestResponse create(CreateRequestRequest req, Long currentEmployeeId) {
        Employee employee = findEmployeeOrThrow(currentEmployeeId);
        Asset asset = assetRepository.findActiveById(req.getAssetId())
                .orElseThrow(() -> new AssetNotFoundException(req.getAssetId()));

        if (req.getType() == RequestType.ASSIGN) {
            // Validate asset AVAILABLE và không có request đang chờ
            if (asset.getStatus() != AssetStatus.AVAILABLE) {
                throw new AssetNotAvailableException(asset.getId());
            }
            if (requestRepository.existsActiveRequestForAsset(asset.getId())) {
                throw new AssetNotAvailableException(asset.getId());
            }
        } else {
            // RETURN — chỉ được trả thiết bị mình đang giữ
            if (!Objects.equals(asset.getAssignedTo() != null ? asset.getAssignedTo().getId() : null,
                    currentEmployeeId)) {
                throw new BusinessException("REQUEST_ASSET_NOT_ASSIGNED_TO_YOU",
                        "Bạn không thể yêu cầu trả thiết bị không phải do mình đang giữ");
            }
        }

        Request request = new Request();
        request.setType(req.getType());
        request.setStatus(RequestStatus.PENDING);
        request.setAsset(asset);
        request.setEmployee(employee);
        request.setNote(req.getNote());
        Request saved = requestRepository.save(request);

        log.info("Request created: id={}, type={}, assetId={}, employeeId={}",
                saved.getId(), req.getType(), asset.getId(), currentEmployeeId);

        // Notify Manager của phòng ban employee
        if (employee.getDepartment() != null && employee.getDepartment().getManager() != null) {
            notificationService.notify(
                    employee.getDepartment().getManager().getId(),
                    NotificationType.REQUEST_CREATED,
                    "Nhân viên " + employee.getFullName() + " đã tạo yêu cầu "
                            + req.getType().name() + " cho thiết bị " + asset.getCode());
        }

        return requestMapper.toResponse(saved);
    }

    // ── Approve ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public RequestResponse approve(Long requestId, Long managerId) {
        log.debug("approve() called: requestId={}, managerId={}", requestId, managerId);
        Request request = findActiveOrThrow(requestId);

        validateNotTerminal(request);
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RequestAlreadyProcessedException();
        }

        // Scope check: Manager chỉ duyệt request của nhân viên PHÒNG MÌNH
        checkManagerScope(request.getEmployee(), managerId);

        Employee manager = findEmployeeOrThrow(managerId);
        request.setStatus(RequestStatus.APPROVED);
        request.setApprovedBy(manager);
        request.setApprovedAt(Instant.now());
        requestRepository.save(request);

        log.info("Request approved: requestId={}, managerId={}", requestId, managerId);

        notificationService.notify(request.getEmployee().getId(), NotificationType.REQUEST_APPROVED,
                "Yêu cầu " + request.getType().name() + " thiết bị "
                        + request.getAsset().getCode() + " đã được duyệt");

        return requestMapper.toResponse(request);
    }

    // ── Reject ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public RequestResponse reject(Long requestId, RejectRequestRequest req, Long managerId) {
        Request request = findActiveOrThrow(requestId);

        validateNotTerminal(request);
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RequestAlreadyProcessedException();
        }

        checkManagerScope(request.getEmployee(), managerId);

        Employee manager = findEmployeeOrThrow(managerId);
        request.setStatus(RequestStatus.REJECTED);
        request.setApprovedBy(manager);
        request.setApprovedAt(Instant.now());
        request.setRejectionReason(req.getRejectionReason());
        requestRepository.save(request);

        log.info("Request rejected: requestId={}, managerId={}, reason={}",
                requestId, managerId, req.getRejectionReason());

        notificationService.notify(request.getEmployee().getId(), NotificationType.REQUEST_REJECTED,
                "Yêu cầu " + request.getType().name() + " thiết bị "
                        + request.getAsset().getCode() + " đã bị từ chối: " + req.getRejectionReason());

        return requestMapper.toResponse(request);
    }

    // ── Fulfill ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public RequestResponse fulfill(Long requestId, Long itStaffId) {
        Request request = findActiveOrThrow(requestId);

        validateNotTerminal(request);
        if (request.getStatus() != RequestStatus.APPROVED) {
            throw new BusinessException("REQUEST_ALREADY_PROCESSED", "Yêu cầu chưa được duyệt");
        }

        Employee itStaff = findEmployeeOrThrow(itStaffId);
        Asset asset = assetRepository.findActiveById(request.getAsset().getId())
                .orElseThrow(() -> new AssetNotFoundException(request.getAsset().getId()));

        if (request.getType() == RequestType.ASSIGN) {
            fulfillAssign(request, asset, itStaff);
        } else {
            fulfillReturn(request, asset, itStaff);
        }

        log.info("Request fulfilled: requestId={}, type={}, itStaffId={}",
                requestId, request.getType(), itStaffId);

        notificationService.notify(request.getEmployee().getId(), NotificationType.REQUEST_FULFILLED,
                "Yêu cầu " + request.getType().name() + " thiết bị "
                        + asset.getCode() + " đã được thực hiện");

        return requestMapper.toResponse(request);
    }

    private void fulfillAssign(Request request, Asset asset, Employee itStaff) {
        // Double-check asset vẫn AVAILABLE (phòng race condition)
        if (asset.getStatus() != AssetStatus.AVAILABLE) {
            throw new AssetNotAvailableException(asset.getId());
        }

        Employee employee = request.getEmployee();

        // Tạo assignment history
        AssetAssignmentHistory history = new AssetAssignmentHistory();
        history.setAsset(asset);
        history.setEmployee(employee);
        history.setRequest(request);
        history.setAssignedAt(Instant.now());
        historyRepository.save(history);

        // Cập nhật asset
        asset.setStatus(AssetStatus.ASSIGNED);
        asset.setAssignedTo(employee);
        assetRepository.save(asset);

        // Cập nhật request
        request.setStatus(RequestStatus.FULFILLED);
        request.setFulfilledBy(itStaff);
        request.setFulfilledAt(Instant.now());
        requestRepository.save(request);
    }

    private void fulfillReturn(Request request, Asset asset, Employee itStaff) {
        // Validate toàn vẹn dữ liệu
        if (!Objects.equals(asset.getAssignedTo() != null ? asset.getAssignedTo().getId() : null,
                request.getEmployee().getId())) {
            throw new BusinessException("REQUEST_ASSET_NOT_ASSIGNED_TO_YOU",
                    "Thiết bị không còn thuộc về nhân viên trong yêu cầu này");
        }

        // Đóng dòng history đang mở
        historyRepository.findOpenAssignment(asset.getId()).ifPresent(h -> {
            h.setReturnedAt(Instant.now());
            historyRepository.save(h);
        });

        // Cập nhật asset
        asset.setStatus(AssetStatus.AVAILABLE);
        asset.setAssignedTo(null);
        assetRepository.save(asset);

        // Cập nhật request
        request.setStatus(RequestStatus.FULFILLED);
        request.setFulfilledBy(itStaff);
        request.setFulfilledAt(Instant.now());
        requestRepository.save(request);
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public RequestResponse cancel(Long requestId, Long currentEmployeeId) {
        Request request = findActiveOrThrow(requestId);

        // Chỉ Employee chính chủ được hủy
        if (!Objects.equals(request.getEmployee().getId(), currentEmployeeId)) {
            throw new ForbiddenException("AUTH_FORBIDDEN", "Bạn không có quyền hủy yêu cầu này");
        }
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BusinessException("REQUEST_CANNOT_CANCEL_NON_PENDING",
                    "Chỉ có thể hủy yêu cầu khi đang ở trạng thái chờ duyệt");
        }

        request.setStatus(RequestStatus.CANCELLED);
        requestRepository.save(request);

        log.info("Request cancelled: requestId={}, employeeId={}", requestId, currentEmployeeId);
        return requestMapper.toResponse(request);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Request findActiveOrThrow(Long id) {
        return requestRepository.findActiveById(id)
                .orElseThrow(() -> new RequestNotFoundException(id));
    }

    private Employee findEmployeeOrThrow(Long id) {
        return employeeRepository.findActiveById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    private void validateNotTerminal(Request request) {
        if (request.getStatus().isTerminal()) {
            throw new RequestAlreadyProcessedException();
        }
    }

    /**
     * Bước 2 scope check: Manager chỉ duyệt request của nhân viên PHÒNG MÌNH phụ trách.
     */
    private void checkManagerScope(Employee requester, Long managerId) {
        if (requester.getDepartment() == null) {
            throw new ForbiddenException("AUTH_OUT_OF_SCOPE",
                    "Bạn không có quyền duyệt yêu cầu này");
        }
        Department dept = departmentRepository.findActiveById(requester.getDepartment().getId())
                .orElseThrow(() -> new DepartmentNotFoundException(requester.getDepartment().getId()));

        if (!Objects.equals(dept.getManager() != null ? dept.getManager().getId() : null, managerId)) {
            log.warn("Manager {} attempted to approve request outside their department scope", managerId);
            throw new ForbiddenException("AUTH_OUT_OF_SCOPE",
                    "Bạn không có quyền duyệt yêu cầu của nhân viên không thuộc phòng ban mình quản lý");
        }
    }
}
