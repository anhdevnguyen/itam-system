package com.vanh.itam.request.service;

import com.vanh.itam.request.dto.request.CreateRequestRequest;
import com.vanh.itam.request.dto.request.RejectRequestRequest;
import com.vanh.itam.request.dto.response.RequestResponse;
import com.vanh.itam.request.entity.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RequestService {
    Page<RequestResponse> getAll(RequestStatus status, Long employeeId, Long branchId, Pageable pageable);
    RequestResponse getById(Long id);
    RequestResponse create(CreateRequestRequest request, Long currentEmployeeId);
    RequestResponse approve(Long requestId, Long managerId);
    RequestResponse reject(Long requestId, RejectRequestRequest request, Long managerId);
    RequestResponse fulfill(Long requestId, Long itStaffId);
    RequestResponse cancel(Long requestId, Long currentEmployeeId);
}
