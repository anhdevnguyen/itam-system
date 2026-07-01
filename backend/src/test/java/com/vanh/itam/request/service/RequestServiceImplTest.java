package com.vanh.itam.request.service;

import com.vanh.itam.asset.entity.Asset;
import com.vanh.itam.asset.entity.AssetStatus;
import com.vanh.itam.asset.exception.AssetNotAvailableException;
import com.vanh.itam.asset.repository.AssetAssignmentHistoryRepository;
import com.vanh.itam.asset.repository.AssetRepository;
import com.vanh.itam.audit.entity.NotificationType;
import com.vanh.itam.audit.service.NotificationService;
import com.vanh.itam.common.exception.BusinessException;
import com.vanh.itam.common.exception.ForbiddenException;
import com.vanh.itam.employee.entity.Branch;
import com.vanh.itam.employee.entity.Department;
import com.vanh.itam.employee.entity.Employee;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho RequestServiceImpl.
 * Mock toàn bộ Repository và external service — không chạm DB thật.
 * Kiểm tra mọi nhánh logic theo docs/07-BUSINESS-RULES.md mục 1.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RequestServiceImpl — Unit Tests")
class RequestServiceImplTest {

    @Mock private RequestRepository requestRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private AssetAssignmentHistoryRepository historyRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private RequestMapper requestMapper;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private RequestServiceImpl requestService;

    // ── Test fixtures ──────────────────────────────────────────────────────

    private Employee buildEmployee(Long id, Long deptId) {
        Employee e = new Employee();
        e.setId(id);
        e.setFullName("Test Employee " + id);
        if (deptId != null) {
            Department dept = new Department();
            dept.setId(deptId);
            e.setDepartment(dept);
        }
        return e;
    }

    private Employee buildManager(Long id) {
        Employee m = new Employee();
        m.setId(id);
        m.setFullName("Test Manager " + id);
        return m;
    }

    private Department buildDepartment(Long id, Long managerId) {
        Department d = new Department();
        d.setId(id);
        if (managerId != null) {
            Employee mgr = buildManager(managerId);
            d.setManager(mgr);
        }
        return d;
    }

    private Asset buildAsset(Long id, AssetStatus status) {
        Asset a = new Asset();
        a.setId(id);
        a.setCode("HN-LAP-000" + id);
        a.setStatus(status);
        Branch branch = new Branch();
        branch.setId(1L);
        a.setBranch(branch);
        return a;
    }

    private Request buildRequest(Long id, RequestStatus status, RequestType type,
                                  Long employeeId, Long assetId) {
        Request r = new Request();
        r.setId(id);
        r.setStatus(status);
        r.setType(type);
        r.setEmployee(buildEmployee(employeeId, 10L));
        r.setAsset(buildAsset(assetId, AssetStatus.AVAILABLE));
        return r;
    }

    // ── Approve ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("approve()")
    class ApproveTests {

        @Test
        @DisplayName("Nên ném ForbiddenException khi Manager không thuộc phòng ban của requester")
        void approve_shouldThrowForbidden_whenManagerOutsideDepartmentScope() {
            Long requestId = 1L;
            Long wrongManagerId = 99L;

            Request request = buildRequest(requestId, RequestStatus.PENDING, RequestType.ASSIGN, 5L, 1L);
            Department dept = buildDepartment(10L, 1L); // manager_id = 1, không phải 99

            when(requestRepository.findActiveById(requestId)).thenReturn(Optional.of(request));
            when(departmentRepository.findActiveById(10L)).thenReturn(Optional.of(dept));

            assertThatThrownBy(() -> requestService.approve(requestId, wrongManagerId))
                    .isInstanceOf(ForbiddenException.class);

            verify(requestRepository, never()).save(any());
            verify(notificationService, never()).notify(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Nên ném RequestAlreadyProcessedException khi request đã FULFILLED (terminal state)")
        void approve_shouldThrow_whenRequestIsTerminal() {
            Long requestId = 1L;
            Request request = buildRequest(requestId, RequestStatus.FULFILLED, RequestType.ASSIGN, 5L, 1L);

            when(requestRepository.findActiveById(requestId)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> requestService.approve(requestId, 1L))
                    .isInstanceOf(RequestAlreadyProcessedException.class);
        }

        @Test
        @DisplayName("Nên ném RequestAlreadyProcessedException khi request đã APPROVED (không phải PENDING)")
        void approve_shouldThrow_whenStatusIsNotPending() {
            Long requestId = 1L;
            Long managerId = 1L;

            Request request = buildRequest(requestId, RequestStatus.APPROVED, RequestType.ASSIGN, 5L, 1L);

            when(requestRepository.findActiveById(requestId)).thenReturn(Optional.of(request));
            // APPROVED là non-terminal nhưng != PENDING → throw trước khi gọi checkManagerScope()
            // → departmentRepository không được gọi → không cần stub

            assertThatThrownBy(() -> requestService.approve(requestId, managerId))
                    .isInstanceOf(RequestAlreadyProcessedException.class);
        }

        @Test
        @DisplayName("Nên approve thành công và gửi notification khi manager đúng scope")
        void approve_shouldSucceed_whenManagerInScope() {
            Long requestId = 1L;
            Long managerId = 10L;
            Long employeeId = 5L;

            Request request = buildRequest(requestId, RequestStatus.PENDING, RequestType.ASSIGN, employeeId, 1L);
            Department dept = buildDepartment(10L, managerId);
            Employee manager = buildManager(managerId);
            RequestResponse mockResponse = mock(RequestResponse.class);

            when(requestRepository.findActiveById(requestId)).thenReturn(Optional.of(request));
            when(departmentRepository.findActiveById(10L)).thenReturn(Optional.of(dept));
            when(employeeRepository.findActiveById(managerId)).thenReturn(Optional.of(manager));
            when(requestRepository.save(any(Request.class))).thenReturn(request);
            when(requestMapper.toResponse(any(Request.class))).thenReturn(mockResponse);

            RequestResponse result = requestService.approve(requestId, managerId);

            assertThat(result).isEqualTo(mockResponse);
            assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);
            assertThat(request.getApprovedBy()).isEqualTo(manager);
            assertThat(request.getApprovedAt()).isNotNull();
            verify(notificationService).notify(eq(employeeId), eq(NotificationType.REQUEST_APPROVED),
                    anyString(), eq(requestId));
        }

        @Test
        @DisplayName("Nên ném ForbiddenException khi employee không thuộc phòng ban nào")
        void approve_shouldThrowForbidden_whenEmployeeHasNoDepartment() {
            Long requestId = 1L;
            Long managerId = 10L;

            // Employee không có department
            Request request = buildRequest(requestId, RequestStatus.PENDING, RequestType.ASSIGN, 5L, 1L);
            request.getEmployee().setDepartment(null);

            when(requestRepository.findActiveById(requestId)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> requestService.approve(requestId, managerId))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("không có quyền");
        }
    }

    // ── Reject ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reject()")
    class RejectTests {

        @Test
        @DisplayName("Nên reject thành công với lý do hợp lệ")
        void reject_shouldSucceed_whenValidManagerAndReason() {
            Long requestId = 1L;
            Long managerId = 10L;
            Long employeeId = 5L;

            Request request = buildRequest(requestId, RequestStatus.PENDING, RequestType.ASSIGN, employeeId, 1L);
            Department dept = buildDepartment(10L, managerId);
            Employee manager = buildManager(managerId);
            RejectRequestRequest rejectReq = new RejectRequestRequest();
            rejectReq.setRejectionReason("Thiết bị đã hết");
            RequestResponse mockResponse = mock(RequestResponse.class);

            when(requestRepository.findActiveById(requestId)).thenReturn(Optional.of(request));
            when(departmentRepository.findActiveById(10L)).thenReturn(Optional.of(dept));
            when(employeeRepository.findActiveById(managerId)).thenReturn(Optional.of(manager));
            when(requestRepository.save(any(Request.class))).thenReturn(request);
            when(requestMapper.toResponse(any(Request.class))).thenReturn(mockResponse);

            RequestResponse result = requestService.reject(requestId, rejectReq, managerId);

            assertThat(result).isEqualTo(mockResponse);
            assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);
            assertThat(request.getRejectionReason()).isEqualTo("Thiết bị đã hết");
            verify(notificationService).notify(eq(employeeId), eq(NotificationType.REQUEST_REJECTED),
                    anyString(), eq(requestId));
        }

        @Test
        @DisplayName("Nên ném RequestAlreadyProcessedException khi reject request đã CANCELLED")
        void reject_shouldThrow_whenRequestAlreadyCancelled() {
            Long requestId = 1L;
            Long managerId = 10L;
            Request request = buildRequest(requestId, RequestStatus.CANCELLED, RequestType.ASSIGN, 5L, 1L);

            when(requestRepository.findActiveById(requestId)).thenReturn(Optional.of(request));
            // CANCELLED là terminal state → validateNotTerminal() throw trước khi checkManagerScope()
            // → departmentRepository không được gọi → không cần stub

            assertThatThrownBy(() -> requestService.reject(requestId, new RejectRequestRequest(), managerId))
                    .isInstanceOf(RequestAlreadyProcessedException.class);
        }
    }

    // ── Fulfill ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("fulfill()")
    class FulfillTests {

        @Test
        @DisplayName("Nên fulfill ASSIGN thành công: cập nhật asset → ASSIGNED, tạo history")
        void fulfill_shouldAssignAsset_whenRequestTypeIsAssign() {
            Long requestId = 1L;
            Long itStaffId = 20L;
            Long employeeId = 5L;
            Long assetId = 1L;

            Asset asset = buildAsset(assetId, AssetStatus.AVAILABLE);
            Employee employee = buildEmployee(employeeId, 10L);
            Employee itStaff = buildManager(itStaffId);

            Request request = new Request();
            request.setId(requestId);
            request.setStatus(RequestStatus.APPROVED);
            request.setType(RequestType.ASSIGN);
            request.setEmployee(employee);
            request.setAsset(asset);

            RequestResponse mockResponse = mock(RequestResponse.class);

            when(requestRepository.findActiveById(requestId)).thenReturn(Optional.of(request));
            when(employeeRepository.findActiveById(itStaffId)).thenReturn(Optional.of(itStaff));
            when(assetRepository.findActiveById(assetId)).thenReturn(Optional.of(asset));
            when(requestRepository.save(any())).thenReturn(request);
            when(assetRepository.save(any())).thenReturn(asset);
            when(requestMapper.toResponse(any())).thenReturn(mockResponse);

            RequestResponse result = requestService.fulfill(requestId, itStaffId);

            assertThat(result).isEqualTo(mockResponse);
            assertThat(asset.getStatus()).isEqualTo(AssetStatus.ASSIGNED);
            assertThat(asset.getAssignedTo()).isEqualTo(employee);
            assertThat(request.getStatus()).isEqualTo(RequestStatus.FULFILLED);
            assertThat(request.getFulfilledBy()).isEqualTo(itStaff);
            verify(historyRepository).save(any());
            verify(notificationService).notify(eq(employeeId), eq(NotificationType.REQUEST_FULFILLED),
                    anyString(), eq(requestId));
        }

        @Test
        @DisplayName("Nên ném BusinessException khi fulfill request chưa APPROVED")
        void fulfill_shouldThrow_whenRequestNotApproved() {
            Long requestId = 1L;
            Request request = buildRequest(requestId, RequestStatus.PENDING, RequestType.ASSIGN, 5L, 1L);

            when(requestRepository.findActiveById(requestId)).thenReturn(Optional.of(request));
            // PENDING → validateNotTerminal() pass → status != APPROVED → throw BusinessException
            // → findEmployeeOrThrow() không được gọi → không cần stub employeeRepository

            assertThatThrownBy(() -> requestService.fulfill(requestId, 1L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Nên ném AssetNotAvailableException khi asset không còn AVAILABLE lúc fulfill")
        void fulfill_shouldThrow_whenAssetNoLongerAvailable() {
            Long requestId = 1L;
            Long itStaffId = 20L;
            Long assetId = 1L;

            Asset asset = buildAsset(assetId, AssetStatus.ASSIGNED); // đã bị ai chiếm
            Employee employee = buildEmployee(5L, 10L);
            Employee itStaff = buildManager(itStaffId);

            Request request = new Request();
            request.setId(requestId);
            request.setStatus(RequestStatus.APPROVED);
            request.setType(RequestType.ASSIGN);
            request.setEmployee(employee);
            request.setAsset(asset);

            when(requestRepository.findActiveById(requestId)).thenReturn(Optional.of(request));
            when(employeeRepository.findActiveById(itStaffId)).thenReturn(Optional.of(itStaff));
            when(assetRepository.findActiveById(assetId)).thenReturn(Optional.of(asset));

            assertThatThrownBy(() -> requestService.fulfill(requestId, itStaffId))
                    .isInstanceOf(AssetNotAvailableException.class);
        }
    }

    // ── Cancel ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancel()")
    class CancelTests {

        @Test
        @DisplayName("Nên cancel thành công khi employee chính chủ và request PENDING")
        void cancel_shouldSucceed_whenOwnerAndPending() {
            Long requestId = 1L;
            Long employeeId = 5L;

            Request request = buildRequest(requestId, RequestStatus.PENDING, RequestType.ASSIGN, employeeId, 1L);
            RequestResponse mockResponse = mock(RequestResponse.class);

            when(requestRepository.findActiveById(requestId)).thenReturn(Optional.of(request));
            when(requestRepository.save(any())).thenReturn(request);
            when(requestMapper.toResponse(any())).thenReturn(mockResponse);

            RequestResponse result = requestService.cancel(requestId, employeeId);

            assertThat(result).isEqualTo(mockResponse);
            assertThat(request.getStatus()).isEqualTo(RequestStatus.CANCELLED);
        }

        @Test
        @DisplayName("Nên ném ForbiddenException khi employee không phải chủ request")
        void cancel_shouldThrowForbidden_whenNotOwner() {
            Long requestId = 1L;
            Long ownerId = 5L;
            Long anotherEmployeeId = 99L;

            Request request = buildRequest(requestId, RequestStatus.PENDING, RequestType.ASSIGN, ownerId, 1L);

            when(requestRepository.findActiveById(requestId)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> requestService.cancel(requestId, anotherEmployeeId))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("Nên ném BusinessException khi cancel request không ở trạng thái PENDING")
        void cancel_shouldThrow_whenRequestNotPending() {
            Long requestId = 1L;
            Long employeeId = 5L;

            Request request = buildRequest(requestId, RequestStatus.APPROVED, RequestType.ASSIGN, employeeId, 1L);

            when(requestRepository.findActiveById(requestId)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> requestService.cancel(requestId, employeeId))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ── Create ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("Nên ném AssetNotAvailableException khi tạo ASSIGN request cho asset không AVAILABLE")
        void create_shouldThrow_whenAssetNotAvailable() {
            Long employeeId = 5L;
            Long assetId = 1L;

            Asset asset = buildAsset(assetId, AssetStatus.ASSIGNED); // đang bị cấp phát
            Employee employee = buildEmployee(employeeId, 10L);

            CreateRequestRequest req = new CreateRequestRequest();
            req.setType(RequestType.ASSIGN);
            req.setAssetId(assetId);

            when(employeeRepository.findActiveById(employeeId)).thenReturn(Optional.of(employee));
            when(assetRepository.findActiveById(assetId)).thenReturn(Optional.of(asset));

            assertThatThrownBy(() -> requestService.create(req, employeeId))
                    .isInstanceOf(AssetNotAvailableException.class);
        }

        @Test
        @DisplayName("Nên ném AssetNotAvailableException khi asset AVAILABLE nhưng đã có request đang pending")
        void create_shouldThrow_whenActiveRequestExistsForAsset() {
            Long employeeId = 5L;
            Long assetId = 1L;

            Asset asset = buildAsset(assetId, AssetStatus.AVAILABLE);
            Employee employee = buildEmployee(employeeId, 10L);

            CreateRequestRequest req = new CreateRequestRequest();
            req.setType(RequestType.ASSIGN);
            req.setAssetId(assetId);

            when(employeeRepository.findActiveById(employeeId)).thenReturn(Optional.of(employee));
            when(assetRepository.findActiveById(assetId)).thenReturn(Optional.of(asset));
            when(requestRepository.existsActiveRequestForAsset(assetId)).thenReturn(true);

            assertThatThrownBy(() -> requestService.create(req, employeeId))
                    .isInstanceOf(AssetNotAvailableException.class);
        }

        @Test
        @DisplayName("Nên tạo request thành công và notify manager khi ASSIGN asset AVAILABLE")
        void create_shouldSucceed_andNotifyManager_whenAssetAvailable() {
            Long employeeId = 5L;
            Long assetId = 1L;
            Long managerId = 10L;

            Asset asset = buildAsset(assetId, AssetStatus.AVAILABLE);
            Employee employee = buildEmployee(employeeId, 10L);
            Department dept = buildDepartment(10L, managerId);
            employee.setDepartment(dept);
            dept.setManager(buildManager(managerId));

            CreateRequestRequest req = new CreateRequestRequest();
            req.setType(RequestType.ASSIGN);
            req.setAssetId(assetId);

            Request savedRequest = buildRequest(1L, RequestStatus.PENDING, RequestType.ASSIGN, employeeId, assetId);
            RequestResponse mockResponse = mock(RequestResponse.class);

            when(employeeRepository.findActiveById(employeeId)).thenReturn(Optional.of(employee));
            when(assetRepository.findActiveById(assetId)).thenReturn(Optional.of(asset));
            when(requestRepository.existsActiveRequestForAsset(assetId)).thenReturn(false);
            when(requestRepository.save(any())).thenReturn(savedRequest);
            when(requestMapper.toResponse(any())).thenReturn(mockResponse);

            RequestResponse result = requestService.create(req, employeeId);

            assertThat(result).isEqualTo(mockResponse);
            verify(requestRepository).save(any(Request.class));
            verify(notificationService).notify(eq(managerId), eq(NotificationType.REQUEST_CREATED),
                    anyString(), anyLong());
        }

        @Test
        @DisplayName("Nên ném BusinessException khi tạo RETURN request nhưng asset không thuộc employee đó")
        void create_shouldThrow_whenReturnAssetNotAssignedToEmployee() {
            Long employeeId = 5L;
            Long assetId = 1L;

            Asset asset = buildAsset(assetId, AssetStatus.ASSIGNED);
            // asset.assignedTo là null → không thuộc employee 5
            Employee employee = buildEmployee(employeeId, 10L);

            CreateRequestRequest req = new CreateRequestRequest();
            req.setType(RequestType.RETURN);
            req.setAssetId(assetId);

            when(employeeRepository.findActiveById(employeeId)).thenReturn(Optional.of(employee));
            when(assetRepository.findActiveById(assetId)).thenReturn(Optional.of(asset));

            assertThatThrownBy(() -> requestService.create(req, employeeId))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ── getById ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getById()")
    class GetByIdTests {

        @Test
        @DisplayName("Nên ném RequestNotFoundException khi request không tồn tại")
        void getById_shouldThrow_whenNotFound() {
            when(requestRepository.findActiveById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> requestService.getById(999L))
                    .isInstanceOf(RequestNotFoundException.class);
        }

        @Test
        @DisplayName("Nên trả RequestResponse khi request tồn tại")
        void getById_shouldReturnResponse_whenFound() {
            Long requestId = 1L;
            Request request = buildRequest(requestId, RequestStatus.PENDING, RequestType.ASSIGN, 5L, 1L);
            RequestResponse mockResponse = mock(RequestResponse.class);

            when(requestRepository.findActiveById(requestId)).thenReturn(Optional.of(request));
            when(requestMapper.toResponse(request)).thenReturn(mockResponse);

            assertThat(requestService.getById(requestId)).isEqualTo(mockResponse);
        }
    }
}
